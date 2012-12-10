package com.sk89q.craftbook.util;

public class ItemInfo {

    public int id;
    public byte data;

    public ItemInfo(int id, byte data) {

        this.id = id;
        this.data = data;
    }

    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    public byte getData () {
        return data;
    }

    public void setData (byte data) {
        this.data = data;
    }

    public static ItemInfo parseFromString(String string) {

        int id = Integer.parseInt(string.split(":")[0]);
        byte data = -1;

        try {
            data = Byte.parseByte(string.split(":")[1]);
        }
        catch(Exception e){}

        return new ItemInfo(id,data);
    }

    @Override
    public String toString() {

        return id + ":" + data;
    }

    @Override
    public int hashCode() {

        return (id * 1103515245 + 12345 ^ data * 1103515245 + 12345) * 1103515245 + 12345;
    }

    @Override
    public boolean equals(Object object) {

        if(object instanceof ItemInfo) {

            ItemInfo it = (ItemInfo) object;
            if(it.getId() == getId()) {
                if(it.getData() == getData() || it.getData() == -1 || getData() == -1)
                    return true;
            }
        }
        return false;
    }
}