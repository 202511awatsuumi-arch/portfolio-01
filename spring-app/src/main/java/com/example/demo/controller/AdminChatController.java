package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequestMapping("/admin/chat")
@PreAuthorize("isAuthenticated()")
public class AdminChatController {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final String SYSTEM_PROMPT =
        "あなたはKAORI MUSUBIの管理画面専属アシスタントです。\n" +
        "KAORI MUSUBIは「香りで紡ぐ、特別な時間」をコンセプトに、\n" +
        "オーダーメイド調香体験を提供するサービスです。\n\n" +
        "【あなたの役割】\n" +
        "管理者の業務効率を上げるために以下を行います：\n\n" +
        "1. 問い合わせへの返信文作成\n" +
        "   - 丁寧で温かみのある文体\n" +
        "   - KAORI MUSUBIのブランドトーンを維持\n" +
        "   - 具体的な次のアクションを含める\n\n" +
        "2. 文章のトーン調整\n" +
        "   - 「丁寧に」「カジュアルに」「短く」などの指示に従う\n\n" +
        "3. 問い合わせの要約・分類\n" +
        "   - 要点を箇条書きで簡潔にまとめる\n" +
        "   - 緊急度・対応優先度を判断して伝える\n\n" +
        "4. コンテンツ生成\n" +
        "   - ワークショップ・体験プランの説明文\n" +
        "   - SNS投稿文・メールマガジン文章\n\n" +
        "【回答ルール】\n" +
        "- 必ず日本語で回答する\n" +
        "- 回答は具体的かつ実用的にする\n" +
        "- 返信文を生成する際は「---」で区切って本文のみ提示する\n" +
        "- 修正依頼には必ず応じる\n" +
        "- 不明な点は確認する";

    private String getScreenPrompt(String screenType) {
        return switch (screenType) {
            case "users_list" -> """
                あなたは現在「ユーザー一覧画面」のAIアシスタントです。
                この画面では、登録済みユーザーの確認、ユーザー登録画面への移動、ユーザー編集、ユーザー削除などを支援します。
                「ユーザー管理の操作方法」を聞かれたら、この画面でユーザーを一覧確認し、必要に応じて新規登録・編集・削除を行う流れとして説明してください。
                「ADMINとUSERの違い」を聞かれたら、ADMINは管理者権限、USERは一般ユーザー権限として説明してください。
                「パスワードポリシー」を聞かれたら、推測されにくい文字列、安全な管理、使い回しを避けることを説明してください。
                「削除の注意点」を聞かれたら、対象ユーザーを間違えないこと、削除前に必要性を確認すること、管理者アカウントを誤って削除しないことを説明してください。
                画面上で確認できない機能は断定しないでください。
                回答は日本語で、短く、箇条書き中心にしてください。
                """;
            case "users_new" -> """
                あなたは現在「ユーザー登録画面」のAIアシスタントです。
                この画面では、新しいユーザー情報の入力と登録を支援します。
                「ユーザー登録の入力方法」を聞かれたら、ユーザー名、パスワード、ロールなど必要項目を入力して登録する流れとして説明してください。
                「パスワード要件」を聞かれたら、推測されにくい文字列、十分な長さ、使い回しを避けることを説明してください。
                「ADMINとUSERの違い」を聞かれたら、ADMINは管理者権限、USERは一般ユーザー権限として説明してください。
                登録前に入力内容とロールが正しいか確認するよう案内してください。
                画面上で確認できない機能は断定しないでください。
                回答は日本語で、短く、箇条書き中心にしてください。
                """;
            case "users_edit" -> """
                あなたは現在「ユーザー編集画面」のAIアシスタントです。
                この画面では、既存ユーザー情報の編集を支援します。
                「ユーザー編集の方法」を聞かれたら、対象ユーザーの情報を確認し、必要な項目を変更して保存する流れとして説明してください。
                「パスワード変更方法」を聞かれたら、新しいパスワードを入力する場合のみ変更される可能性があること、空欄時の扱いは画面仕様に従うこと、変更前に確認することを説明してください。
                「ADMINとUSERの違い」を聞かれたら、ADMINは管理者権限、USERは一般ユーザー権限として説明してください。
                ロール変更やパスワード変更は影響が大きいため、保存前に確認するよう案内してください。
                画面上で確認できない機能は断定しないでください。
                回答は日本語で、短く、箇条書き中心にしてください。
                """;
            case "inquiries_deleted" -> """
                あなたは現在「削除済み問い合わせ一覧画面」のAIアシスタントです。
                この画面では、削除済み問い合わせの確認と復元操作を支援します。
                「復元方法」を聞かれたら、削除済み一覧から対象の問い合わせを確認し、復元操作を行う流れとして説明してください。
                「復元時の注意点」を聞かれたら、対象を間違えないこと、復元後に通常の問い合わせ一覧へ戻る可能性があること、必要な問い合わせだけ復元することを説明してください。
                「この画面でできること」を聞かれたら、削除済み問い合わせの確認と復元が中心だと説明してください。
                「復元と完全削除の違い」を聞かれたら、復元は元に戻す操作、完全削除は戻せなくなる可能性がある操作として説明してください。ただし、この画面に完全削除機能があると断定しないでください。
                画面上で確認できない機能は断定しないでください。
                回答は日本語で、短く、箇条書き中心にしてください。
                """;
            default -> "";
        };
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody Map<String, Object> body) {

        String userMessage = (String) body.get("message");
        List<Map<String, Object>> history =
            (List<Map<String, Object>>) body.getOrDefault("history", new ArrayList<>());
        String screenType = String.valueOf(body.getOrDefault("screenType", ""));

        RestTemplate restTemplate = new RestTemplate();

        List<Map<String, Object>> contents = new ArrayList<>();
        String screenPrompt = getScreenPrompt(screenType);

        contents.add(Map.of(
            "role", "user",
            "parts", List.of(Map.of("text", SYSTEM_PROMPT))));
        contents.add(Map.of(
            "role", "model",
            "parts", List.of(Map.of("text",
                "承知しました。KAORI MUSUBIの管理業務をサポートします。"))));

        if (!screenPrompt.isBlank()) {
            contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", screenPrompt))));
        }

        contents.addAll(history);

        contents.add(Map.of(
            "role", "user",
            "parts", List.of(Map.of("text", userMessage))));

        Map<String, Object> requestBody = Map.of(
            "contents", contents,
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 2048
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request =
            new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                GEMINI_URL + apiKey, request, Map.class);

            List candidates = (List) response.getBody().get("candidates");
            Map candidate = (Map) candidates.get(0);
            Map contentMap = (Map) candidate.get("content");
            List parts = (List) contentMap.get("parts");
            Map firstPart = (Map) parts.get(0);
            String text = (String) firstPart.get("text");

            return ResponseEntity.ok(Map.of("reply", text));

        } catch (Exception e) {
            String errorMsg;
            String msg = e.getMessage();
            if (msg != null && msg.contains("429")) {
                errorMsg = "リクエストが多すぎます。しばらく待ってから再試行してください。";
            } else if (msg != null && msg.contains("403")) {
                errorMsg = "APIキーが無効です。管理者に連絡してください。";
            } else if (msg != null && msg.contains("404")) {
                errorMsg = "AIモデルが見つかりません。管理者に連絡してください。";
            } else {
                errorMsg = "エラーが発生しました。しばらく待ってから再試行してください。";
            }
            return ResponseEntity.status(500)
                .body(Map.of("reply", errorMsg));
        }
    }
}
