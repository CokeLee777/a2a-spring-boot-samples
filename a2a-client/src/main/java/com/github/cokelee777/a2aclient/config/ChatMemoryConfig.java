package com.github.cokelee777.a2aclient.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for chat conversation memory.
 * <p>
 * This configuration sets up the conversation memory system that maintains chat history
 * on a per-session basis. It uses an in-memory repository with a sliding window of recent
 * messages to provide conversation context to the LLM while managing memory usage.
 * </p>
 */
@Configuration
public class ChatMemoryConfig {

	/**
	 * Creates an in-memory chat memory repository.
	 * <p>
	 * This repository stores chat histories for multiple sessions in memory. Sessions are
	 * identified by their unique session ID.
	 * </p>
	 * @return a new {@code InMemoryChatMemoryRepository} instance
	 */
	@Bean
	public ChatMemoryRepository chatMemoryRepository() {
		return new InMemoryChatMemoryRepository();
	}

	/**
	 * Creates a chat memory instance with a message window.
	 * <p>
	 * This memory maintains a sliding window of the most recent messages (up to 20
	 * messages per session) to provide conversation context without storing the entire
	 * chat history.
	 * </p>
	 * @param chatMemoryRepository the repository backing this memory instance
	 * @return a configured {@code MessageWindowChatMemory} instance with a maximum of 20
	 * messages
	 */
	@Bean
	public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
		return MessageWindowChatMemory.builder().chatMemoryRepository(chatMemoryRepository).maxMessages(20).build();
	}

}
