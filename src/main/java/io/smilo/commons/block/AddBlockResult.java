//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package io.smilo.commons.block;

public class AddBlockResult {
    private final AddResultType type;
    private final Block block;
    private final String message;

    public AddBlockResult(Block block, AddResultType type, String message) {
        this.type = type;
        this.message = message;
        this.block = block;
    }

    public AddResultType getType() {
        return this.type;
    }

    public Block getBlock() {
        return this.block;
    }

    public String getMessage() {
        return this.message;
    }
}
