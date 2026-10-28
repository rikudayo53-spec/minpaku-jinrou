"""民泊人狼の3Dブロック・アイテムモデル（Minecraft の JSON モデル）を作る。

  python3 gen_models.py <src/main/resources>

ブロックは「正面 = 北（-Z）」で作り、blockstate で向きを回す。
"""
import json
import os
import sys

OUT = sys.argv[1]
NS = "minpaku"
FACES = ["north", "south", "east", "west", "up", "down"]


def box(fr, to, tex, faces=None, rot=None, skip=()):
    """fr/to は 0〜16。tex は "#名前" か、面ごとの dict"""
    f = {}
    for face in FACES:
        if face in skip:
            continue
        t = tex[face] if isinstance(tex, dict) else tex
        if t is None:
            continue
        f[face] = {"texture": t}
    el = {"from": list(fr), "to": list(to), "faces": f}
    if rot:
        el["rotation"] = rot
    return el


def roty(angle, origin):
    return {"angle": angle, "axis": "y", "origin": list(origin)}


def rotz(angle, origin):
    return {"angle": angle, "axis": "z", "origin": list(origin)}


def rotx(angle, origin):
    return {"angle": angle, "axis": "x", "origin": list(origin)}


def write(path, data):
    full = os.path.join(OUT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as fp:
        json.dump(data, fp, ensure_ascii=False, indent=1)


def block(name, textures, elements, parent="minecraft:block/block"):
    write(f"assets/{NS}/models/block/{name}.json",
          {"parent": parent, "textures": textures, "elements": elements})
    write(f"assets/{NS}/blockstates/{name}.json", {"variants": {
        "facing=north": {"model": f"{NS}:block/{name}"},
        "facing=east": {"model": f"{NS}:block/{name}", "y": 90},
        "facing=south": {"model": f"{NS}:block/{name}", "y": 180},
        "facing=west": {"model": f"{NS}:block/{name}", "y": 270},
    }})
    write(f"assets/{NS}/models/item/{name}.json", {"parent": f"{NS}:block/{name}"})


ITEM_DISPLAY = {
    "gui": {"rotation": [20, -30, 0], "translation": [0, 0, 0], "scale": [0.85, 0.85, 0.85]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]},
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 2, 1], "scale": [0.6, 0.6, 0.6]},
    "thirdperson_lefthand": {"rotation": [0, -90, 0], "translation": [0, 2, 1], "scale": [0.6, 0.6, 0.6]},
    "firstperson_righthand": {"rotation": [0, -70, 10], "translation": [1, 2, 0], "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, 110, -10], "translation": [1, 2, 0], "scale": [0.6, 0.6, 0.6]},
}


def item(name, textures, elements, display=None):
    write(f"assets/{NS}/models/item/{name}.json",
          {"textures": textures, "elements": elements, "display": display or ITEM_DISPLAY})


def ring_layers(cx, cz, layers, tex):
    """(y0, y1, 半径) の段を積んで丸っぽくする"""
    out = []
    for y0, y1, r in layers:
        out.append(box((cx - r, y0, cz - r), (cx + r, y1, cz + r), tex))
    return out


# =============================================================== 呼び鈴（緊急ボタン）
def call_bell():
    tex = {"particle": "minecraft:block/gold_block", "wood": "minecraft:block/dark_oak_planks",
           "gold": "minecraft:block/gold_block", "iron": "minecraft:block/iron_block",
           "cloth": "minecraft:block/red_wool"}
    els = [
        # 木の台と赤い座布団
        box((2, 0, 2), (14, 2, 14), "#wood"),
        box((3, 2, 3), (13, 3, 13), "#cloth"),
    ]
    els += ring_layers(8, 8, [(3, 4, 4.5), (4, 6, 4), (6, 7, 3.5), (7, 8, 2.5), (8, 8.5, 1.5)], "#gold")
    # 斜めにもう1組重ねて丸く見せる
    for y0, y1, r in [(3, 4, 4.5), (4, 6, 4), (6, 7, 3.5)]:
        els.append(box((8 - r, y0, 8 - r), (8 + r, y1, 8 + r), "#gold", rot=roty(45, (8, 8, 8))))
    els += [
        box((7.5, 8.5, 7.5), (8.5, 10, 8.5), "#iron"),       # 押す棒
        box((6.5, 10, 6.5), (9.5, 10.8, 9.5), "#iron"),       # 押すところ
    ]
    block("call_bell", tex, els)


# =============================================================== 床下点検口（ベント）
def floor_hatch():
    tex = {"particle": f"{NS}:block/floor_hatch", "top": f"{NS}:block/floor_hatch",
           "iron": "minecraft:block/iron_block", "dark": "minecraft:block/black_concrete"}
    els = [
        box((0, 0, 0), (16, 1, 16), {"north": "#iron", "south": "#iron", "east": "#iron", "west": "#iron",
                                     "up": "#top", "down": "#iron"}),
        # 取っ手
        box((6, 1, 7), (10, 1.6, 7.6), "#iron"),
        box((6, 1, 8.4), (10, 1.6, 9), "#iron"),
        box((6, 1, 7), (6.6, 1.6, 9), "#iron"),
        box((9.4, 1, 7), (10, 1.6, 9), "#iron"),
        # すき間からのぞく暗がり
        box((1, 0.9, 1), (15, 1.05, 1.6), "#dark"),
    ]
    block("floor_hatch", tex, els)


# =============================================================== 配電盤
def breaker():
    tex = {"particle": "minecraft:block/iron_block", "box": "minecraft:block/light_gray_concrete",
           "face": f"{NS}:block/breaker_face", "pipe": "minecraft:block/gray_concrete",
           "warn": "minecraft:block/yellow_concrete"}
    els = [
        box((2, 2, 11), (14, 15, 16), {"north": "#face", "south": "#box", "east": "#box", "west": "#box",
                                       "up": "#box", "down": "#box"}),
        # ふたのふち
        box((1.5, 1.5, 10.5), (14.5, 2.5, 11), "#box"),
        box((1.5, 14.5, 10.5), (14.5, 15.5, 11), "#box"),
        # 配管
        box((7, 15, 12.5), (9, 16, 14.5), "#pipe"),
        box((7, 0, 12.5), (9, 2, 14.5), "#pipe"),
        # 注意シール
        box((10, 12.5, 10.8), (13, 14, 11), "#warn"),
    ]
    block("breaker", tex, els)


# =============================================================== ガスの元栓
def gas_valve():
    tex = {"particle": "minecraft:block/yellow_concrete", "pipe": "minecraft:block/yellow_concrete",
           "metal": "minecraft:block/iron_block", "red": "minecraft:block/red_concrete",
           "base": "minecraft:block/gray_concrete"}
    els = [
        box((5, 0, 5), (11, 1, 11), "#base"),
        box((7, 1, 7), (9, 11, 9), "#pipe"),                  # 縦の管
        box((6.5, 6, 6.5), (9.5, 9, 9.5), "#metal"),          # 栓の本体
        box((9, 7, 7), (16, 9, 9), "#pipe"),                  # 横へのびる管
        box((7.5, 9, 7.5), (8.5, 10.5, 8.5), "#metal"),       # 軸
        box((4, 10.5, 7.2), (12, 11.5, 8.8), "#red", rot=roty(22.5, (8, 11, 8))),  # 赤いハンドル
        box((7, 11, 7), (9, 12, 9), "#red"),
    ]
    block("gas_valve", tex, els)


# =============================================================== ボイラー
def boiler():
    tex = {"particle": "minecraft:block/iron_block", "body": "minecraft:block/white_concrete",
           "trim": "minecraft:block/light_gray_concrete", "pipe": "minecraft:block/copper_block",
           "gauge": f"{NS}:block/gauge", "fire": f"{NS}:block/fire_window", "dark": "minecraft:block/gray_concrete"}
    els = [
        box((3, 0, 3), (13, 1, 13), "#dark"),
        box((3, 1, 3), (13, 14, 13), "#body"),
        box((4, 1, 4), (12, 14, 12), "#body", rot=roty(45, (8, 8, 8))),
        box((2.5, 13, 2.5), (13.5, 14.5, 13.5), "#trim"),
        box((2.5, 1, 2.5), (13.5, 2, 13.5), "#trim"),
        # 正面の炎の窓と圧力計
        box((6, 3, 1.8), (10, 6, 3), {"north": "#fire", "south": "#dark", "east": "#dark", "west": "#dark",
                                       "up": "#dark", "down": "#dark"}),
        box((6, 9, 1.8), (10, 12, 3), {"north": "#gauge", "south": "#trim", "east": "#trim", "west": "#trim",
                                        "up": "#trim", "down": "#trim"}),
        # 煙突と配管
        box((7, 14.5, 7), (9, 16, 9), "#pipe"),
        box((13, 4, 7), (16, 6, 9), "#pipe"),
        box((13, 9, 7), (16, 11, 9), "#pipe"),
    ]
    block("boiler", tex, els)


# =============================================================== 配線盤
def wiring_panel():
    tex = {"particle": "minecraft:block/dark_oak_planks", "frame": "minecraft:block/dark_oak_planks",
           "face": f"{NS}:block/wiring_face", "red": "minecraft:block/red_concrete",
           "blue": "minecraft:block/blue_concrete", "yellow": "minecraft:block/yellow_concrete",
           "pink": "minecraft:block/pink_concrete", "metal": "minecraft:block/iron_block"}
    els = [
        box((1, 1, 13), (15, 15, 16), {"north": "#face", "south": "#frame", "east": "#frame", "west": "#frame",
                                       "up": "#frame", "down": "#frame"}),
        box((0.5, 0.5, 12.5), (15.5, 1.5, 13), "#frame"),
        box((0.5, 14.5, 12.5), (15.5, 15.5, 13), "#frame"),
    ]
    # 飛び出した配線
    for i, c in enumerate(["#red", "#blue", "#yellow", "#pink"]):
        y = 3 + i * 3
        els.append(box((2.5, y, 12), (3.5, y + 1, 13), "#metal"))
        els.append(box((12.5, y, 12), (13.5, y + 1, 13), "#metal"))
        sag = [0.5, 1.0, 0.5][i % 3]
        els.append(box((3.5, y - sag, 12.2), (12.5, y + 0.6 - sag, 12.8), c))
    block("wiring_panel", tex, els)


# =============================================================== 予約台帳（文机の上の帳面）
def ledger():
    tex = {"particle": "minecraft:block/oak_planks", "wood": "minecraft:block/oak_planks",
           "page": f"{NS}:block/ledger_page", "cover": "minecraft:block/brown_wool",
           "ink": "minecraft:block/black_concrete", "red": "minecraft:block/red_concrete"}
    els = [
        box((1, 9, 2), (15, 10, 14), "#wood"),                 # 天板
        box((2, 0, 3), (3, 9, 4), "#wood"), box((13, 0, 3), (14, 9, 4), "#wood"),
        box((2, 0, 12), (3, 9, 13), "#wood"), box((13, 0, 12), (14, 9, 13), "#wood"),
        box((2, 3, 3.5), (14, 4, 4), "#wood"),
        # 帳面（開いた本）
        box((3, 10, 4), (13, 10.5, 12), "#cover"),
        box((3.3, 10.5, 4.3), (8, 11, 11.7), {"up": "#page", "north": "#page", "south": "#page",
                                              "east": "#page", "west": "#page", "down": "#page"},
            rot=rotz(-22.5, (8, 10.5, 8))),
        box((8, 10.5, 4.3), (12.7, 11, 11.7), {"up": "#page", "north": "#page", "south": "#page",
                                               "east": "#page", "west": "#page", "down": "#page"},
            rot=rotz(22.5, (8, 10.5, 8))),
        # 筆
        box((10, 10.6, 12), (10.8, 11.2, 15), "#ink", rot=roty(22.5, (10, 11, 13))),
        box((10, 10.6, 11.4), (10.8, 11.2, 12), "#red", rot=roty(22.5, (10, 11, 13))),
    ]
    block("ledger", tex, els)


# =============================================================== 布団（たたんで積んである）
def futon():
    tex = {"particle": f"{NS}:block/futon_top", "top": f"{NS}:block/futon_top",
           "white": "minecraft:block/white_wool", "blue": "minecraft:block/blue_wool",
           "pillow": "minecraft:block/white_concrete"}
    els = [
        box((1, 0, 3), (15, 2.5, 13), {"north": "#white", "south": "#white", "east": "#white", "west": "#white",
                                        "up": "#white", "down": "#white"}),
        box((1.5, 2.5, 3.2), (14.5, 4.5, 12.8), "#blue"),
        box((1, 4.5, 3), (15, 6.5, 13), {"north": "#white", "south": "#white", "east": "#white", "west": "#white",
                                          "up": "#top", "down": "#white"}),
        # まくら
        box((4, 6.5, 6), (12, 8.5, 10), "#pillow", rot=roty(-22.5, (8, 7, 8))),
    ]
    block("futon", tex, els)


# =============================================================== かまど（ご飯を炊く）
def kamado():
    tex = {"particle": "minecraft:block/terracotta", "clay": "minecraft:block/terracotta",
           "white": "minecraft:block/white_terracotta", "fire": f"{NS}:block/fire_window",
           "pot": "minecraft:block/black_concrete", "lid": "minecraft:block/spruce_planks",
           "dark": "minecraft:block/black_terracotta"}
    els = [
        box((1, 0, 1), (15, 8, 15), {"north": "#white", "south": "#white", "east": "#white", "west": "#white",
                                      "up": "#clay", "down": "#clay"}),
        box((0.5, 7, 0.5), (15.5, 8.5, 15.5), "#clay"),
        # 焚き口
        box((5, 1, 0.5), (11, 5, 1), {"north": "#fire", "south": "#dark", "east": "#dark", "west": "#dark",
                                       "up": "#dark", "down": "#dark"}),
        box((4.5, 5, 0.3), (11.5, 5.8, 1), "#dark"),
        # 羽釜とふた
        box((4, 8.5, 4), (12, 11.5, 12), "#pot"),
        box((3, 9.5, 3), (13, 10.2, 13), "#pot"),
        box((3.5, 11.5, 3.5), (12.5, 12.5, 12.5), "#lid"),
        box((7, 12.5, 6), (9, 13.5, 10), "#lid"),
    ]
    block("kamado", tex, els)


# =============================================================== 風呂釜（お風呂を沸かす）
def bath_stove():
    tex = {"particle": "minecraft:block/bricks", "brick": "minecraft:block/bricks",
           "fire": f"{NS}:block/fire_window", "iron": "minecraft:block/iron_block",
           "dark": "minecraft:block/black_concrete", "copper": "minecraft:block/copper_block",
           "wood": "minecraft:block/oak_log"}
    els = [
        box((2, 0, 3), (14, 11, 15), "#brick"),
        box((5, 2, 2.5), (11, 7, 3), {"north": "#fire", "south": "#dark", "east": "#dark", "west": "#dark",
                                       "up": "#dark", "down": "#dark"}),
        box((4.5, 7, 2.2), (11.5, 8, 3), "#iron"),
        box((10, 11, 10), (13, 16, 13), "#copper"),            # 煙突
        # 薪
        box((1, 0, 0), (7, 2, 2), "#wood", rot=roty(22.5, (4, 1, 1))),
        box((9, 0, 0.5), (15, 2, 2.5), "#wood", rot=roty(-22.5, (12, 1, 1.5))),
        box((4, 2, 0.5), (10, 4, 2.5), "#wood"),
    ]
    block("bath_stove", tex, els)


# =============================================================== 薪割り台
def chopping_block():
    tex = {"particle": "minecraft:block/oak_log", "side": "minecraft:block/oak_log",
           "top": "minecraft:block/oak_log_top", "handle": "minecraft:block/stripped_oak_log",
           "blade": "minecraft:block/iron_block", "split": "minecraft:block/birch_log"}
    cut = {"north": "#side", "south": "#side", "east": "#side", "west": "#side", "up": "#top", "down": "#top"}
    els = [
        box((3, 0, 3), (13, 8, 13), cut),
        box((3, 0, 3), (13, 8, 13), cut, rot=roty(45, (8, 4, 8))),
        # 刺さった斧
        box((7.5, 8, 7.5), (8.5, 17, 8.5), "#handle", rot=rotz(-22.5, (8, 8, 8))),
        box((5.5, 7, 7.6), (9.5, 10, 8.4), "#blade"),
        # 割った薪
        box((0, 0, 12), (6, 2, 14), "#split", rot=roty(22.5, (3, 1, 13))),
        box((11, 0, 0), (13, 2, 6), "#split"),
    ]
    block("chopping_block", tex, els)


# =============================================================== 畑（野菜を収穫する）
def vegetable_patch():
    tex = {"particle": f"{NS}:block/soil", "soil": f"{NS}:block/soil", "wood": "minecraft:block/spruce_planks",
           "carrot": "minecraft:block/orange_concrete", "leaf": "minecraft:block/moss_block",
           "cabbage": "minecraft:block/lime_concrete", "radish": "minecraft:block/white_concrete"}
    els = [
        box((0, 0, 0), (16, 4, 16), {"north": "#wood", "south": "#wood", "east": "#wood", "west": "#wood",
                                      "up": "#soil", "down": "#wood"}),
    ]
    spots = [(3, 3, "c"), (8, 3, "k"), (13, 3, "r"), (3, 8, "k"), (8, 8, "c"), (13, 8, "c"),
             (3, 13, "r"), (8, 13, "c"), (13, 13, "k")]
    for x, z, kind in spots:
        if kind == "c":
            els.append(box((x - 1, 4, z - 1), (x + 1, 5, z + 1), "#carrot"))
            els.append(box((x - 0.5, 5, z - 1.5), (x + 0.5, 8, z + 1.5), "#leaf", rot=roty(45, (x, 6, z))))
            els.append(box((x - 1.5, 5, z - 0.5), (x + 1.5, 7, z + 0.5), "#leaf", rot=roty(45, (x, 6, z))))
        elif kind == "k":
            els.append(box((x - 2, 4, z - 2), (x + 2, 7, z + 2), "#cabbage"))
            els.append(box((x - 2.5, 4, z - 1), (x + 2.5, 6, z + 1), "#leaf"))
        else:
            els.append(box((x - 1, 4, z - 1), (x + 1, 6, z + 1), "#radish"))
            els.append(box((x - 0.5, 6, z - 0.5), (x + 0.5, 9, z + 0.5), "#leaf"))
    block("vegetable_patch", tex, els)


# =============================================================== ほこり
def dust_pile():
    tex = {"particle": f"{NS}:block/dust", "dust": f"{NS}:block/dust", "ball": "minecraft:block/light_gray_wool"}
    els = [
        box((2, 0, 3), (8, 1, 9), "#dust"),
        box((7, 0, 6), (14, 1.2, 12), "#dust"),
        box((4, 1, 5), (7, 2, 8), "#ball"),
        box((9, 1, 8), (12, 2.5, 11), "#ball"),
        box((11, 0, 2), (13, 1, 4), "#ball"),
        box((3, 0, 11), (5, 0.8, 13), "#dust"),
    ]
    block("dust_pile", tex, els)


# =============================================================== 玄関マット（スタート地点）
def entrance_mat():
    tex = {"particle": f"{NS}:block/entrance_mat", "mat": f"{NS}:block/entrance_mat",
           "edge": "minecraft:block/brown_wool", "geta": "minecraft:block/spruce_planks",
           "strap": "minecraft:block/red_wool"}
    els = [
        box((1, 0, 3), (15, 1, 13), {"north": "#edge", "south": "#edge", "east": "#edge", "west": "#edge",
                                      "up": "#mat", "down": "#edge"}),
        # げた
        box((10, 1, 4), (12, 2, 8), "#geta"),
        box((12.5, 1, 4.5), (14.5, 2, 8.5), "#geta"),
        box((10.2, 2, 5.5), (11.8, 2.4, 5.9), "#strap"),
        box((12.7, 2, 6), (14.3, 2.4, 6.4), "#strap"),
    ]
    block("entrance_mat", tex, els)


# =============================================================== アイテム
def manager_key():
    tex = {"particle": "minecraft:block/gold_block", "gold": "minecraft:block/gold_block",
           "tag": "minecraft:block/oak_planks", "cord": "minecraft:block/red_wool"}
    els = [
        # 輪っか
        box((5, 11, 7.5), (11, 12, 8.5), "#gold"),
        box((5, 15, 7.5), (11, 16, 8.5), "#gold"),
        box((5, 12, 7.5), (6, 15, 8.5), "#gold"),
        box((10, 12, 7.5), (11, 15, 8.5), "#gold"),
        # 軸と歯
        box((7.5, 2, 7.5), (8.5, 11, 8.5), "#gold"),
        box((8.5, 2, 7.5), (10.5, 3, 8.5), "#gold"),
        box((8.5, 4, 7.5), (10, 5, 8.5), "#gold"),
        # 木の札とひも
        box((1, 6, 7.7), (5, 12, 8.3), "#tag"),
        box((4.5, 11, 7.8), (6, 11.6, 8.2), "#cord"),
    ]
    item("manager_key", tex, els)


def seer_crystal():
    tex = {"particle": "minecraft:block/light_blue_stained_glass", "glass": "minecraft:block/light_blue_stained_glass",
           "core": "minecraft:block/amethyst_block", "base": "minecraft:block/gold_block",
           "cloth": "minecraft:block/purple_wool"}
    els = [
        box((4, 0, 4), (12, 2, 12), "#base"),
        box((3, 2, 3), (13, 3, 13), "#cloth"),
        box((6, 5, 6), (10, 9, 10), "#core"),
    ]
    els += ring_layers(8, 8, [(3, 4, 3), (4, 5, 4), (5, 9, 4.5), (9, 10, 4), (10, 11, 3), (11, 11.5, 2)], "#glass")
    item("seer_crystal", tex, els)


def knight_charm():
    tex = {"particle": f"{NS}:item/charm_cloth", "cloth": f"{NS}:item/charm_cloth",
           "red": "minecraft:block/red_wool", "gold": "minecraft:block/gold_block",
           "cord": "minecraft:block/white_wool"}
    els = [
        box((4.5, 2, 7), (11.5, 12, 9), {"north": "#cloth", "south": "#cloth", "east": "#red", "west": "#red",
                                          "up": "#red", "down": "#red"}),
        box((5, 12, 7.2), (11, 13, 8.8), "#red"),
        box((6, 13, 7.5), (10, 13.5, 8.5), "#gold"),
        # ひも
        box((7.5, 13.5, 7.7), (8.5, 16, 8.3), "#cord"),
        box((6.5, 15, 7.7), (9.5, 16, 8.3), "#cord"),
    ]
    item("knight_charm", tex, els)


def sabotage_talisman():
    tex = {"particle": f"{NS}:item/talisman_paper", "paper": f"{NS}:item/talisman_paper",
           "back": "minecraft:block/white_concrete", "red": "minecraft:block/red_concrete"}
    els = [
        box((5, 1, 7.7), (11, 8, 8.2), {"north": "#paper", "south": "#back", "east": "#back", "west": "#back",
                                         "up": "#back", "down": "#back"}, rot=rotx(-22.5, (8, 8, 8))),
        box((5, 8, 7.7), (11, 15, 8.2), {"north": "#paper", "south": "#back", "east": "#back", "west": "#back",
                                          "up": "#back", "down": "#back"}),
        box((5, 14.5, 7.6), (11, 15.5, 8.3), "#red"),
    ]
    item("sabotage_talisman", tex, els)


for fn in [call_bell, floor_hatch, breaker, gas_valve, boiler, wiring_panel, ledger, futon, kamado,
           bath_stove, chopping_block, vegetable_patch, dust_pile, entrance_mat,
           manager_key, seer_crystal, knight_charm, sabotage_talisman]:
    fn()
print("models ok")
