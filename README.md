# 香り結び（KAORI MUSUBI）ポートフォリオ

「浅草での調香ワークショップ」を想定した、**店舗紹介サイト + お問い合わせ（予約相談）管理**のポートフォリオ作品です。  
フロントは静的なページ構成（日本語/英語）を用意し、`spring-app/` では **Spring Boot + Thymeleaf** でページ配信とお問い合わせの保存・管理（管理画面）を行います。

> 注: このリポジトリ内の文言・住所などはサンプル表現（例: `東京都台東区浅草X-X-X`）を含みます。

---

## 概要
- 対象: 観光客が「ワークショップ内容を理解して予約相談できる」ことを目的としたサイト
- 構成:
  - ルート直下: 静的HTML/CSS/JS一式（デザイン確認・プロトタイプ用途）
  - `spring-app/`: Spring Bootアプリ（Thymeleafでページ表示、フォーム送信、管理画面）
- 管理者向け機能: 管理画面でお問い合わせを一覧・詳細確認し、編集/削除（論理削除）/復元できます

---

## 主な機能
### サイト（来訪者向け）
- ページ構成（例）: トップ / ワークショップ / こだわり / 香りの魅力 / アクセス / お問い合わせ
- **日本語/英語ページ**（`/en/*`）
- スマホ対応ナビゲーション（ハンバーガーメニュー）
- お問い合わせフォーム
  - 問い合わせ種別に応じて入力項目を出し分け
  - 入力チェック（フロント側）

### 管理画面（運営者向け / Spring Boot）
- ログイン（Spring Security）
- お問い合わせ（inquiries）
  - 一覧（ページング）
  - 詳細表示
  - 内容編集
  - 論理削除・復元（削除済み一覧）

---

## 使用技術
### フロント
- HTML / CSS / JavaScript
- Google Fonts（Material Symbols）

### バックエンド（`spring-app/`）
- Java 17
- Spring Boot（Web / Thymeleaf / Validation / Security / Data JPA）
- H2 Database（ファイルDB）
- Flyway（DBマイグレーション）
- MyBatis（設定・マッパーを含む）
- Maven（Maven Wrapper）

### インフラ/実行
- Docker（`Dockerfile` でSpring BootのJARをビルドして実行）

---

## セットアップ手順
### 1) 静的ページを確認する（プロトタイプ）
- `index.html` をブラウザで開きます  
  - 相対パスの都合上、VS Code の Live Server 等で開くと確認しやすいです（任意）

### 2) Spring Boot アプリとして起動する（推奨）
前提: Java 17 がインストールされていること

```powershell
cd spring-app
.\mvnw.cmd spring-boot:run
```

- サイト: `http://localhost:8080/`
- 管理画面ログイン: `http://localhost:8080/admin/login`
- H2 Console（有効）: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:file:./data/portfolio-db`（`spring-app/src/main/resources/application.properties`）

管理者ユーザー（初期値）は `spring-app/src/main/resources/application.properties` に定義があります。  
（第三者レビュー時にログインできるよう、必要に応じて値を共有してください）

### 3) Docker で起動する（任意）
```powershell
docker build -t portfolio-01 .
docker run --rm -p 10000:10000 portfolio-01
```

- アクセス: `http://localhost:10000/`

---

## ディレクトリ構成
（主要部分のみ）

```text
.
├─ index.html / about.html / works.html ...   # 静的HTML（プロトタイプ）
├─ en/                                       # 英語ページ（静的）
├─ css/ / js/ / images/                      # 静的アセット
├─ docs/                                     # 企画〜設計〜テスト等のドキュメント（HTML）
├─ design/                                   # デザイン関連（※内容はリポジトリ参照）
├─ prompts/                                  # 作業ログ/プロンプト類（HTML）
└─ spring-app/                               # Spring Boot アプリ本体
   ├─ src/main/java/...                      # コントローラ/サービス等
   ├─ src/main/resources/templates/          # Thymeleaf テンプレート（JP/EN, admin）
   ├─ src/main/resources/static/             # アセット（css/js/images/docs 等）
   └─ data/                                  # H2 DB ファイル保存先（ローカル実行時）
```

---

## 工夫した点
- **問い合わせ種別に応じたフォームUI**（予約相談向け項目の出し分け）で、入力負担を下げる設計にしました
- 日本語/英語のページを用意し、観光客向け想定の導線（予約CTA、必要情報の整理）を意識しました
- 管理画面で「一覧→詳細→編集」「削除→復元」の運用を想定し、**論理削除**の導線を用意しました
- `docs/` に要件・設計・テスト等の資料をHTMLでまとめ、制作プロセスが追える形にしています

---

## 今後の課題
- フロントのフォーム送信結果（成功/失敗）のUIを、サーバー応答に合わせてより明確にする（アクセシビリティ含む）
- 管理画面の権限設計（管理者/スタッフ等）や監査ログなど、運用を想定した強化
- 本番運用を想定した環境変数化（DB/管理者初期値など）と、デプロイ手順の明文化

---

## 公開URL
- （未記載）  
  https://portfolio-01-iw7m.onrender.com
