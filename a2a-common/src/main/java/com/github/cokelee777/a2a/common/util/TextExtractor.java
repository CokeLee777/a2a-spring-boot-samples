package com.github.cokelee777.a2a.common.util;

import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;

/**
 * Utility class for working with A2A SDK objects.
 *
 * <p>
 * Provides helper methods to extract and process text content from {@link Message} and
 * {@link Task} objects.
 */
public class TextExtractor {

	private TextExtractor() {
	}

	/**
	 * Extracts the plain text content from a {@link Message}.
	 * @param message the message to extract text from
	 * @return the concatenated text of all text parts, or an empty string if none
	 */
	public static String extractFromMessage(Message message) {
		StringBuilder textBuilder = new StringBuilder();
		for (Part<?> part : message.parts()) {
			if (part instanceof TextPart textPart) {
				textBuilder.append(textPart.text());
			}
		}
		return textBuilder.toString();
	}

	/**
	 * Extracts the plain text content from all artifacts of a {@link Task}.
	 * @param task the task whose artifacts to extract text from
	 * @return the concatenated text of all text parts across all artifacts, or an empty
	 * string if there are no artifacts or no text parts
	 */
	public static String extractFromTask(Task task) {
		if (task.artifacts() == null) {
			return "";
		}
		StringBuilder textBuilder = new StringBuilder();
		task.artifacts().forEach(artifact -> artifact.parts().forEach(part -> {
			if (part instanceof TextPart textPart) {
				textBuilder.append(textPart.text());
			}
		}));
		return textBuilder.toString();
	}

}
