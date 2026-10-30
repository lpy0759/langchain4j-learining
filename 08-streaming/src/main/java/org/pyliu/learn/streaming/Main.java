package org.pyliu.learn.streaming;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

/**
 * 模块 08：流式输出（Streaming）。
 *
 * <p>默认的 {@code chat(...)} 是同步的：等模型<b>全部生成完</b>才返回。
 * 流式则是模型一边生成、一边把 token 逐字推给你，用户能立刻看到文字"打字"效果，
 * 体验更好——ChatGPT 那种逐字输出就是流式。
 */
public class Main {

    // 声明式流式：接口方法返回 TokenStream
    interface Streamer {
        TokenStream stream(String message);
    }

    public static void main(String[] args) {
        // 1) 底层流式模型：手动实现回调
        rawStreaming();

        // 2) AiServices 声明式流式：链式注册回调
        aiServiceStreaming();
    }

    private static void rawStreaming() {
        StreamingChatModel model = newStreamingModel();
        System.out.println("=== 底层流式（onPartialResponse 逐字回调）===");
        model.chat("讲一个 100 字左右的小故事", new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partial) {
                System.out.print(partial);   // 每来一个 token 打印一段，不换行
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                System.out.println("\n[流式结束，共收到一次完整响应]");
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });
    }

    private static void aiServiceStreaming() {
        StreamingChatModel model = newStreamingModel();
        System.out.println("=== AiServices 声明式流式（TokenStream）===");
        Streamer streamer = AiServices.builder(Streamer.class)
                .streamingChatModel(model)
                .build();

        streamer.stream("用一句话介绍什么是 Java 虚拟机")
                .onPartialResponse(t -> System.out.print(t))       // 逐字输出
                .onCompleteResponse(r -> System.out.println("\n[完成]"))
                .onError(Throwable::printStackTrace)
                .start();                                          // 必须调用 start 才会触发
    }

    private static StreamingChatModel newStreamingModel() {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先设置环境变量 DEEPSEEK_API_KEY");
        }
        return OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();
    }
}
