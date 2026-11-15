package com.repcheck.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Walks the whole API against a real PostgreSQL (the docker-compose one locally,
 * a service container in CI). Skipped unless REPCHECK_IT=true so a plain
 * "mvnw test" works without a database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "REPCHECK_IT", matches = "true")
class ApiFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private Clock clock;

    private String token;

    @Test
    void fullUserJourney() throws Exception {
        LocalDate today = LocalDate.now(clock);

        // --- auth
        mvc.perform(get("/api/exercises")).andExpect(status().isUnauthorized());
        String email = "it-" + UUID.randomUUID() + "@repcheck.test";
        JsonNode auth = send(post("/api/auth/register"),
                Map.of("email", email, "password", "password123", "displayName", "IT User"), 201);
        token = auth.get("token").asText();
        assertThat(auth.get("user").get("email").asText()).isEqualTo(email);
        send(post("/api/auth/register"),
                Map.of("email", email, "password", "password123", "displayName", "Dup"), 409);
        send(post("/api/auth/login"), Map.of("email", email, "password", "wrong-password"), 401);
        assertThat(send(post("/api/auth/login"), Map.of("email", email, "password", "password123"), 200)
                .get("token").asText()).isNotBlank();
        send(post("/api/auth/register"), Map.of("email", "nope", "password", "x", "displayName", ""), 400);

        // --- exercises
        JsonNode exercises = send(get("/api/exercises"), null, 200);
        assertThat(exercises.size()).isGreaterThanOrEqualTo(60);
        String bench = idByName(exercises, "Barbell Bench Press");
        String squat = idByName(exercises, "Back Squat");
        assertThat(send(get("/api/exercises?muscle=chest&q=bench"), null, 200).size()).isGreaterThanOrEqualTo(3);
        JsonNode custom = send(post("/api/exercises"), Map.of("name", "Landmine Press", "muscleGroup", "shoulders",
                "secondaryMuscles", List.of("TRICEPS"), "equipment", "barbell", "category", "push"), 201);
        assertThat(custom.get("custom").asBoolean()).isTrue();
        send(post("/api/exercises"), Map.of("name", "Bad", "muscleGroup", "NECK",
                "equipment", "x", "category", "PUSH"), 400);

        // --- workouts and the arrow chain
        JsonNode first = send(post("/api/workouts"), workout(today.minusDays(10), "Push A",
                set(bench, 80, 5), set(bench, 80, 5), set(bench, 80, 5), set(squat, 100, 5)), 201);
        assertThat(block(first, bench).get("deltaDirection").asText()).isEqualTo("NEW");
        assertThat(first.get("totalVolumeKg").asDouble()).isEqualTo(80 * 5 * 3 + 100 * 5);
        assertThat(first.get("exercises").get(0).get("exerciseName").asText()).isEqualTo("Barbell Bench Press");

        JsonNode second = send(post("/api/workouts"), workout(today.minusDays(3), "Push B",
                set(bench, 85, 5), set(bench, 85, 5)), 201);
        assertThat(block(second, bench).get("deltaDirection").asText()).isEqualTo("UP");
        assertThat(block(second, bench).get("pr").asBoolean()).isTrue();
        // 80x5 -> 93.33 est. 1RM, 85x5 -> 99.17: +6.26%
        assertThat(block(second, bench).get("deltaPct").asDouble()).isEqualTo(6.26);

        // Back-fill an older, heavier session: the chain must be re-derived.
        JsonNode old = send(post("/api/workouts"), workout(today.minusDays(20), "Old",
                set(bench, 100, 5)), 201);
        JsonNode secondAgain = send(get("/api/workouts/" + second.get("id").asText()), null, 200);
        assertThat(block(secondAgain, bench).get("deltaDirection").asText()).isEqualTo("UP");
        assertThat(block(secondAgain, bench).get("pr").asBoolean()).isFalse();
        JsonNode firstAgain = send(get("/api/workouts/" + first.get("id").asText()), null, 200);
        assertThat(block(firstAgain, bench).get("deltaDirection").asText()).isEqualTo("DOWN");

        send(post("/api/workouts"), workout(today.plusDays(1), "Future", set(bench, 50, 5)), 400);
        send(post("/api/workouts"), Map.of("sets", List.of()), 400);

        JsonNode page = send(get("/api/workouts?page=0&size=2"), null, 200);
        assertThat(page.get("total").asInt()).isEqualTo(3);
        assertThat(page.get("items").size()).isEqualTo(2);
        assertThat(page.get("items").get(0).get("name").asText()).isEqualTo("Push B");
        assertThat(page.get("items").get(0).get("upCount").asInt()).isEqualTo(1);

        JsonNode previous = send(get("/api/workouts/previous?exerciseIds=" + bench + "," + squat), null, 200);
        assertThat(previous.size()).isEqualTo(2);
        assertThat(previous.get(0).get("sets").size()).isEqualTo(2);

        // --- analytics
        JsonNode lifts = send(get("/api/analytics/lifts"), null, 200);
        assertThat(lifts.size()).isEqualTo(2);
        assertThat(lifts.get(0).get("exerciseName").asText()).isEqualTo("Barbell Bench Press");
        assertThat(lifts.get(0).get("deltaDirection").asText()).isEqualTo("UP");
        assertThat(lifts.get(0).get("trend").size()).isEqualTo(3);

        assertThat(send(get("/api/analytics/exercises/" + bench + "/history"), null, 200).size()).isEqualTo(3);

        JsonNode volume = send(get("/api/analytics/volume?date=" + today.minusDays(3)), null, 200);
        assertThat(volume.get("muscles").size()).isEqualTo(10);
        JsonNode chest = muscle(volume, "CHEST");
        assertThat(chest.get("totalSets").asDouble()).isEqualTo(2.0);
        assertThat(chest.get("mev").asInt()).isEqualTo(8);
        assertThat(chest.get("recommendation").asText()).isIn("INCREASE", "DELOAD");
        assertThat(muscle(volume, "TRICEPS").get("totalSets").asDouble()).isEqualTo(1.0);

        JsonNode fatigue = send(get("/api/analytics/fatigue?days=30"), null, 200);
        JsonNode points = fatigue.get("points");
        assertThat(points.size()).isEqualTo(21);
        assertThat(points.get(points.size() - 1).get("date").asText()).isEqualTo(today.toString());
        assertThat(fatigue.get("status").asText()).isNotBlank();

        assertThat(send(get("/api/analytics/heatmap"), null, 200).size()).isEqualTo(3);
        JsonNode balance = send(get("/api/analytics/balance"), null, 200);
        assertThat(balance.get("pushSets").asDouble()).isEqualTo(6.0);
        assertThat(balance.get("legSets").asDouble()).isEqualTo(1.0);
        JsonNode prs = send(get("/api/analytics/prs"), null, 200);
        assertThat(prs.size()).isEqualTo(2);

        // --- delete re-derives everything
        mvc.perform(auth(delete("/api/workouts/" + old.get("id").asText()))).andExpect(status().isNoContent());
        secondAgain = send(get("/api/workouts/" + second.get("id").asText()), null, 200);
        assertThat(block(secondAgain, bench).get("pr").asBoolean()).isTrue();
        assertThat(send(get("/api/analytics/fatigue?days=30"), null, 200).get("points").size()).isEqualTo(11);
        send(get("/api/workouts/" + old.get("id").asText()), null, 404);

        // --- templates
        Map<String, Object> templateBody = new LinkedHashMap<>();
        templateBody.put("name", "PPL");
        templateBody.put("splitType", "PPL");
        templateBody.put("days", List.of(
                Map.of("name", "Push", "exercises", List.of(
                        Map.of("exerciseId", bench, "targetSets", 3, "targetReps", 8))),
                Map.of("name", "Legs", "exercises", List.of(
                        Map.of("exerciseId", squat, "targetSets", 4, "targetReps", 6)))));
        JsonNode template = send(post("/api/templates"), templateBody, 201);
        String templateId = template.get("id").asText();
        assertThat(template.get("days").get(0).get("exercises").get(0).get("exerciseName").asText())
                .isEqualTo("Barbell Bench Press");
        templateBody.put("name", "PPL v2");
        assertThat(send(put("/api/templates/" + templateId), templateBody, 200).get("name").asText())
                .isEqualTo("PPL v2");
        assertThat(send(get("/api/templates"), null, 200).size()).isEqualTo(1);
        assertThat(send(get("/api/templates/" + templateId), null, 200).get("days").size()).isEqualTo(2);
        Map<String, Object> fromTemplate = workout(today, "From template", set(squat, 105, 5));
        fromTemplate.put("templateId", templateId);
        assertThat(send(post("/api/workouts"), fromTemplate, 201).get("templateId").asText()).isEqualTo(templateId);
        mvc.perform(auth(delete("/api/templates/" + templateId))).andExpect(status().isNoContent());
        send(get("/api/templates/" + templateId), null, 404);

        // --- cardio, steps, streaks, body weight, profile
        JsonNode run = send(post("/api/cardio"), Map.of("activityType", "run", "distanceKm", 5.0,
                "durationMinutes", 30, "steps", 6500), 201);
        assertThat(run.get("paceMinPerKm").asDouble()).isEqualTo(6.0);
        send(post("/api/cardio"), Map.of("activityType", "FLY"), 400);
        assertThat(send(get("/api/cardio"), null, 200).size()).isEqualTo(1);
        JsonNode steps = send(get("/api/cardio/steps"), null, 200);
        assertThat(steps.get("totalSteps").asInt()).isEqualTo(6500);
        assertThat(steps.get("days").size()).isEqualTo(7);

        assertThat(send(patch("/api/users/me"), Map.of("weeklyStepsGoal", 5000, "weeklyWorkoutGoal", 1), 200)
                .get("weeklyStepsGoal").asInt()).isEqualTo(5000);
        JsonNode streaks = send(get("/api/streaks"), null, 200);
        assertThat(streaks.get("steps").get("goalMetThisWeek").asBoolean()).isTrue();
        assertThat(streaks.get("steps").get("currentStreak").asInt()).isEqualTo(1);
        assertThat(streaks.get("workout").get("goalMetThisWeek").asBoolean()).isTrue();
        assertThat(streaks.get("workout").get("currentStreak").asInt()).isGreaterThanOrEqualTo(1);
        // Reading twice must not double count.
        assertThat(send(get("/api/streaks"), null, 200)).isEqualTo(streaks);

        send(post("/api/body-weight"), Map.of("weightKg", 78.4), 200);
        send(post("/api/body-weight"), Map.of("weightKg", 78.1), 200);
        assertThat(send(get("/api/body-weight"), null, 200).size()).isEqualTo(1);
        assertThat(send(get("/api/users/me"), null, 200).get("bodyWeightKg").asDouble()).isEqualTo(78.1);

        // --- form check: only clean reps count as progress
        String press = idByName(exercises, "Overhead Press");
        JsonNode clean = send(post("/api/workouts"), workout(today.minusDays(9), "OHP clean",
                set(press, 40, 8)), 201);
        assertThat(block(clean, press).get("formPct").asDouble()).isEqualTo(100.0);
        assertThat(block(clean, press).get("sets").get(0).get("cleanReps").isNull()).isTrue();

        // 45 x 8 with form breaking after rep 5 is credited as 45 x 5 = 52.5 est. 1RM.
        JsonNode sloppy = send(post("/api/workouts"), workout(today.minusDays(6), "OHP heavier",
                formSet(press, 45, 8, 5)), 201);
        JsonNode sloppyBlock = block(sloppy, press);
        assertThat(sloppyBlock.get("bestOneRm").asDouble()).isEqualTo(52.5);
        assertThat(sloppyBlock.get("formPct").asDouble()).isEqualTo(62.5);
        assertThat(sloppyBlock.get("sets").get(0).get("cleanReps").asInt()).isEqualTo(5);
        assertThat(sloppyBlock.get("sets").get(0).get("reps").asInt()).isEqualTo(8);
        // Volume still counts all 8 reps.
        assertThat(sloppy.get("totalVolumeKg").asDouble()).isEqualTo(45 * 8);

        send(post("/api/workouts"), workout(today.minusDays(2), "OHP again", formSet(press, 45, 8, 6)), 201);
        JsonNode pressCard = null;
        for (JsonNode card : send(get("/api/analytics/lifts"), null, 200)) {
            if (press.equals(card.get("exerciseId").asText())) {
                pressCard = card;
            }
        }
        assertThat(pressCard).isNotNull();
        assertThat(pressCard.get("formPct").asDouble()).isEqualTo(75.0);
        assertThat(pressCard.get("bestReps").asInt()).isEqualTo(6);
        assertThat(pressCard.get("formWarning").asText()).contains("Drop back to 40 kg");
        assertThat(pressCard.get("suggestedWeightKg").asDouble()).isEqualTo(40.0);
        JsonNode pressHistory = send(get("/api/analytics/exercises/" + press + "/history"), null, 200);
        assertThat(pressHistory.get(1).get("formPct").asDouble()).isEqualTo(62.5);
        JsonNode lastTime = send(get("/api/workouts/previous?exerciseIds=" + press), null, 200);
        assertThat(lastTime.get(0).get("sets").get(0).get("cleanReps").asInt()).isEqualTo(6);
        send(post("/api/workouts"), workout(today, "Impossible", formSet(press, 45, 8, 9)), 400);

        mvc.perform(auth(delete("/api/cardio/" + run.get("id").asText()))).andExpect(status().isNoContent());
        mvc.perform(auth(delete("/api/exercises/" + custom.get("id").asText()))).andExpect(status().isNoContent());
        send(delete("/api/exercises/" + bench), null, 404);
    }

    // ------------------------------------------------------------- helpers

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }

    private JsonNode send(MockHttpServletRequestBuilder request, Object body, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder prepared = auth(request);
        if (body != null) {
            prepared = prepared.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        String response = mvc.perform(prepared)
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return response.isBlank() ? json.nullNode() : json.readTree(response);
    }

    private static Map<String, Object> set(String exerciseId, double weightKg, int reps) {
        return Map.of("exerciseId", exerciseId, "weightKg", weightKg, "reps", reps);
    }

    private static Map<String, Object> formSet(String exerciseId, double weightKg, int reps, int cleanReps) {
        return Map.of("exerciseId", exerciseId, "weightKg", weightKg, "reps", reps, "cleanReps", cleanReps);
    }

    @SafeVarargs
    private static Map<String, Object> workout(LocalDate date, String name, Map<String, Object>... sets) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("sessionDate", date.toString());
        body.put("sets", new ArrayList<>(List.of(sets)));
        return body;
    }

    private static String idByName(JsonNode exercises, String name) {
        for (JsonNode exercise : exercises) {
            if (name.equals(exercise.get("name").asText())) {
                return exercise.get("id").asText();
            }
        }
        throw new AssertionError("No exercise named " + name);
    }

    private static JsonNode block(JsonNode workout, String exerciseId) {
        for (JsonNode block : workout.get("exercises")) {
            if (exerciseId.equals(block.get("exerciseId").asText())) {
                return block;
            }
        }
        throw new AssertionError("Exercise " + exerciseId + " not in workout");
    }

    private static JsonNode muscle(JsonNode volume, String muscleGroup) {
        for (JsonNode muscle : volume.get("muscles")) {
            if (muscleGroup.equals(muscle.get("muscleGroup").asText())) {
                return muscle;
            }
        }
        throw new AssertionError("No muscle " + muscleGroup);
    }
}
