# Runtime/Knowledge Config (draft)

## 配置場所
- `config/runtime.json`
- `config/knowledge.vanilla.json`

Forge 側の読み込み入口は以下のクラスとする。
- `com.example.examplemod.config.RuntimeConfigLoader`
- `com.example.examplemod.config.KnowledgePackLoader`

`ExampleMod#commonSetup` でロードし、読み込み結果を `ExampleMod.RUNTIME_CONFIG` / `ExampleMod.KNOWLEDGE_PACK` に保持する。

## runtime.json

### 必須キー (最低限)
- `connection` (object)
- `conversation` (object)
- `llm` (object)
- `action` (object)
- `safety` (object)

### キー構成とデフォルト値
```json
{
  "connection": {
    "mode": "offline",
    "endpoint": "",
    "timeoutMs": 5000,
    "retry": {
      "maxAttempts": 3,
      "backoffMs": 1000
    }
  },
  "conversation": {
    "historyLimit": 20,
    "systemPrompt": "",
    "locale": "ja-JP"
  },
  "llm": {
    "provider": "openai",
    "model": "gpt-4o-mini",
    "temperature": 0.7,
    "maxTokens": 1024
  },
  "action": {
    "enabled": true,
    "allowedActions": [],
    "cooldownMs": 500
  },
  "safety": {
    "allowUnsafe": false,
    "redactPii": true,
    "maxRequestsPerMinute": 60
  }
}
```

### バリデーション (最低限)
- 上記必須キーが **存在しない** / **object ではない** 場合はエラー。
- 各フィールドは、型が不正・欠落している場合に **デフォルト値** を適用。

### フェイルセーフ
- `runtime.json` が **存在しない**: ログ警告を出し、デフォルト値で継続。
- `runtime.json` の **JSON 破損・必須キー欠落**: 起動を中止 (例外送出)。

## knowledge.vanilla.json

### 必須キー (最低限)
- `pack` (object)
- `entries` (array)
- `pack.id` (string)
- `pack.version` (string)

### キー構成とデフォルト値
```json
{
  "pack": {
    "id": "vanilla",
    "name": "Vanilla Knowledge",
    "version": "0.0.0",
    "description": ""
  },
  "entries": [
    {
      "id": "",
      "title": "",
      "tags": [],
      "content": "",
      "source": "",
      "enabled": true
    }
  ]
}
```

### バリデーション (最低限)
- 上記必須キーが **存在しない** / **型が不正** な場合はエラー。
- `entries` は配列、要素は object を期待し、非 object はスキップ。

### フェイルセーフ
- `knowledge.vanilla.json` が **存在しない**: ログ警告を出し、空パックで継続。
- `knowledge.vanilla.json` の **JSON 破損・必須キー欠落**: ログエラーのうえ空パックで継続。
