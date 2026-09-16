package ridzzxmc.xclient.core.keybind;

/** A single rebindable key attached to a module. -1 = unbound (GLFW key codes). */
public class Keybind {

    private int key = -1;

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isBound() {
        return key != -1;
    }

    public String getDisplayName() {
        if (!isBound()) return "NONE";
        return org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0) != null
                ? org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0).toUpperCase()
                : "KEY_" + key;
    }
}
