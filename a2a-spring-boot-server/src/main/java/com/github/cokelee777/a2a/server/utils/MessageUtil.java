package com.github.cokelee777.a2a.server.utils;

import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;

/**
 * Utility class for working with A2A {@link Message} objects.
 *
 * <p>
 * Provides helper methods to extract and process message content parts.
 */
public class MessageUtil {

	/**
	 * Extracts the plain text content from a {@link Message}.
	 * @param message the message to extract text from
	 * @return the first text part content, or an empty string if none is found
	 */
	public static String extractTextFromMessage(Message message) {
		StringBuilder textBuilder = new StringBuilder();
		for (Part<?> part : message.parts()) {
			if (part instanceof TextPart textPart) {
				textBuilder.append(textPart.text());
			}
		}
		return textBuilder.toString();
	}

}
