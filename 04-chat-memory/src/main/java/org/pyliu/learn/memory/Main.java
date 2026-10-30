package org.pyliu.learn.memory;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;

/**
 * 模块 04：对话记忆（Chat Memory）。
 *
 * <p>模块 02 里我们是<b>手动</b>把历史消息拼进列表；本模块让 LangChain4j <b>自动</b>帮我们
 * 保存并携带历史，并且可以控制"记住多少"（条数 or token 数），以及"按用户隔离"。
 */
public class Main {

    interface Assistant {
        String chat(String message);
    }

    // 多用户时：用 @MemoryId 标注"以谁的身份记忆"，AiServices 会按该 id 各自维护一份记忆
    interface MultiUserAssistant {
        String chat(@MemoryId String userId, String message);
    }

    public static void main(String[] args) {
        ChatModel model = newModel();

        // 1) 按消息条数记忆：只保留最近 N 条
        ChatMemory windowMemory = MessageWindowChatMemory.withMaxMessages(10);
        Assistant withMemory = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(windowMemory)
                .build();
        withMemory.chat("我叫小明，是一名 Java 工程师。");
        System.out.println("[消息窗口记忆] " + withMemory.chat("我叫什么名字？做什么工作？"));

        // 2) 按 token 数记忆：控制上下文长度、控制成本
        ChatMemory tokenMemory = TokenWindowChatMemory.builder()
                .maxTokens(500, new OpenAiTokenCountEstimator("deepseek-chat"))
                .build();
        Assistant tokenAssistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(tokenMemory)
                .build();
        tokenAssistant.chat("我养了一只叫旺财的狗。");
        System.out.println("[Token窗口记忆] " + tokenAssistant.chat("我的狗叫什么名字？"));

        // 3) 多用户隔离：不同 userId 各自一份记忆，互不串扰
        ChatMemoryProvider provider = id -> MessageWindowChatMemory.withMaxMessages(10);
        MultiUserAssistant multi = AiServices.builder(MultiUserAssistant.class)
                .chatModel(model)
                .chatMemoryProvider(provider)
                .build();
        multi.chat("user-1", "我叫小明");
        multi.chat("user-2", "我叫小红");
        System.out.println("[多用户隔离] user-1 问：" + multi.chat("user-1", "我叫什么名字？"));
        System.out.println("[多用户隔离] user-2 问：" + multi.chat("user-2", "我叫什么名字？"));

        // 4) 对照：没有记忆，同样的问题模型"失忆"
        Assistant noMemory = AiServices.builder(Assistant.class).chatModel(model).build();
        noMemory.chat("我叫小明。");
        System.out.println("[无记忆对照] " + noMemory.chat("我叫什么名字？"));
    }

    private static ChatModel newModel() {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先设置环境变量 DEEPSEEK_API_KEY");
        }
        return OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();
    }
}
