package jp.co.disney.apps.managed.kisekaeapp.catalog.contents;

public enum ContentsValue {

//	//色（色数未定。仮で6色）
//	COLOR_BLUE(1),
//	COLOR_PINK(2),
//	COLOR_RED(3),
//	COLOR_YELLOW(4),
//	COLOR_GREEN(5),
//	COLOR_SKYBLUE(6),
	
	//キャラ（キャラ数未定。仮で9キャラ）
	CHARA_MICKEY(1),
	CHARA_MINNIE(2),
	CHARA_POOH(3),
	CHARA_STITCH(4),
	CHARA_DONALD(5),
	CHARA_DAISY(6),
	CHARA_TINK(7),
	CHARA_OTHER1(8),
	CHARA_OTHER2(9),
	
	//オススメon/off
	PICK_UP_ON(1),
	PICK_UP_OFF(0);	
	
	private int value;
	ContentsValue(int v) { this.value = v; }
    public int getValue() { return value; }


}
