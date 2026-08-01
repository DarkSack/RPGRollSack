package com.sack.rpgroll.npcs.integration;

public class MineSkinResponse {

    public boolean success;
    public Skin skin;

    public static class Skin {
        public String uuid;
        public Texture texture;
    }

    public static class Texture {
        public Data data;
    }

    public static class Data {
        public String value;
        public String signature;
    }

}