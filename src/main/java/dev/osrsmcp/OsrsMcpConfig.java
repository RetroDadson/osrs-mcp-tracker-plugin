package dev.osrsmcp;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("OsrsMcpTracker")
public interface OsrsMcpConfig extends Config {
    @ConfigSection(
            name = "Login",
            description = "Enter the login details you created on the website here",
            position = 0
    )
    String loginSection = "LoginSection";

    @ConfigSection(
            name = "Self Hosted Config",
            description = "Configure your connection to a self hosted server",
            position = 1,
            closedByDefault = true
    )
    String connectionSection = "ConnectionSection";

    @ConfigItem(
            keyName = "loginName",
            name = "Your Name (on the website)",
            description = "This is the name you provided on the website when signing up",
            section = loginSection
    )
    default String loginName() {
        return "";
    }

    @ConfigItem(
            keyName = "token",
            name = "Token",
            description = "Secret token provided by the website. Get this from whoever created the login on the site, or create a new one by visiting the site.",
            secret = true,
            section = loginSection
    )
    default String authorizationToken() {
        return "";
    }

    @ConfigItem(
            keyName = "baseUrlOverride",
            name = "Server base URL override (leave blank to use public server)",
            description = "Overrides the public server URL used to send data. Only change this if you are hosting your own server.",
            section = connectionSection
    )
    default String baseUrlOverride() {
        return "";
    }
}
