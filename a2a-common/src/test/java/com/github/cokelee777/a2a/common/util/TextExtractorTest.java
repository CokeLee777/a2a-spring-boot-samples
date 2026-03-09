package com.github.cokelee777.a2a.common.util;

import io.a2a.spec.Artifact;
import io.a2a.spec.DataPart;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextExtractorTest {

	@Test
	void extractTextFromMessage_withTextParts_returns() {
		Message message = Message.builder()
			.role(Message.Role.ROLE_USER)
			.parts(new TextPart("Hello"), new TextPart(" World"))
			.build();

		String result = TextExtractor.extractFromMessage(message);

		assertEquals("Hello World", result);
	}

	@Test
	void extractTextFromMessage_withNoParts_returnsEmpty() {
		// Message requires at least one part; use a non-text part to test the
		// no-text-output path
		Message message = Message.builder().role(Message.Role.ROLE_USER).parts(new DataPart("some-data")).build();

		String result = TextExtractor.extractFromMessage(message);

		assertEquals("", result);
	}

	@Test
	void extractTextFromTask_withArtifactsContainingText_returns() {
		Artifact artifact = Artifact.builder().artifactId("art-1").parts(new TextPart("refundEligible:true")).build();

		Task task = Task.builder()
			.id("task-1")
			.contextId("ctx-1")
			.status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
			.artifacts(List.of(artifact))
			.build();

		String result = TextExtractor.extractFromTask(task);

		assertEquals("refundEligible:true", result);
	}

	@Test
	void extractFromTask_withNullArtifacts_returnsEmpty() {
		Task task = Task.builder()
			.id("task-1")
			.contextId("ctx-1")
			.status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
			.artifacts(null)
			.build();

		String result = TextExtractor.extractFromTask(task);

		assertEquals("", result);
	}

	@Test
	void extractFromTask_withMultipleArtifactsAndParts_concatenatesAll() {
		Artifact artifact1 = Artifact.builder().artifactId("art-1").parts(new TextPart("foo")).build();
		Artifact artifact2 = Artifact.builder().artifactId("art-2").parts(new TextPart("bar")).build();

		Task task = Task.builder()
			.id("task-1")
			.contextId("ctx-1")
			.status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
			.artifacts(List.of(artifact1, artifact2))
			.build();

		String result = TextExtractor.extractFromTask(task);

		assertEquals("foobar", result);
	}

}
