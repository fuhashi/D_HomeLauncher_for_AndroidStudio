package jp.co.disney.apps.managed.kisekaeapp.launcher;

class ThemeShortcutDef {

    String iconName;
    String packageName;
    String className;
    int screen;
    int cellX;
    int cellY;

    public ThemeShortcutDef() {}

    public ThemeShortcutDef(ThemeShortcutDef def) {
        iconName = def.iconName;
        packageName = def.packageName;
        className = def.className;
        screen = def.screen;
        cellX = def.cellX;
        cellY = def.cellY;
    }
}
