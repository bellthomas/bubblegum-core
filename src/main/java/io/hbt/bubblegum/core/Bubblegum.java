package io.hbt.bubblegum.core;

import io.hbt.bubblegum.core.social.SocialIdentity;

public class Bubblegum {
    public static void main(String[] args) {
        System.out.println("io.hbt.bubblegum.core.Bubblegum");

        SocialIdentity socialIdentity = new SocialIdentity();

        BubblegumNode node1 = new BubblegumNode(socialIdentity);
        BubblegumNode node2 = new BubblegumNode(socialIdentity, "FDEC9F6A63BE7A02DB92CCF9663D15A24662F403", 2000);
        System.out.println(node1.toString());
        System.out.println(node2.toString());

        System.out.println("");
    }
}
