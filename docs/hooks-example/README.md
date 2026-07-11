# verify-before-done — Stop hook example

把「宣稱完成前先驗收」從一句叮嚀變成機器強制:agent 每次要收工時,
這個 hook 會**攔它一次**,逼它把驗收狀態(deploy 了沒、probes 綠了沒)
講清楚才放行。

## 裝法(在 repo 根目錄)

```sh
mkdir -p .claude
cp docs/hooks-example/settings.snippet.json .claude/settings.json
```

開新的 Claude Code session 後生效。之後每當 agent 說「完成」,你會看到
它被攔下、補上一段驗收狀態說明,然後才真正結束。

## 拆掉

刪掉 `.claude/settings.json`(或其中的 Stop 區塊),開新 session。

## 原理

Stop hook 輸出 `{"decision":"block","reason":"..."}` 時,Claude Code 會把
reason 餵回給 agent、要求它回應;payload 裡的 `stop_hook_active` 用來
辨識「這次 stop 已經攔過了」,第二次直接放行——不加這個 guard 會無限
迴圈,session 永遠關不掉。
