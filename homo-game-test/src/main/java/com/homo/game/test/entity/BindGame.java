package com.homo.game.test.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class BindGame {
    public String appId;
    public String appName;
    public String channelId;
    public String gameId;
    public String role;
    public String roleId;

}


