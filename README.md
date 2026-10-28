# 民泊人狼

古民家の民泊を舞台にした、Among Us 風のマイクラ人狼 mod（Fabric 1.21.1）。

## 遊び方（かんたん）
1. クリエイティブタブ「民泊人狼」から、マップ用のブロックを置く
   - 玄関マット（スタート地点）、呼び鈴（緊急ボタン）、床下点検口（ベント）、配電盤、ガスの元栓、ボイラー
   - タスクのブロック（配線盤・予約台帳・布団・かまど・風呂釜・薪割り台・畑・ほこり）
2. 「管理人の鍵」を右クリック → 設定を決めて「ゲーム開始」（OPのみ）
   - コマンドでも可: `/minpaku start` `/minpaku stop` `/minpaku key`

## 開発
- `tools/models/gen_models.py` … 3Dブロック・アイテムのモデルを作る
- `tools/models/gen_textures.py` … オリジナルのテクスチャを作る
- 役職の追加: `game/Role.java`、設定の追加: `game/GameSettings.java`、タスクの追加: `game/MarkerType.java`
