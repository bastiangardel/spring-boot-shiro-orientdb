package com.github.pires.example.dto;

import org.apache.shiro.authc.UsernamePasswordToken;

/**
 * Created by bastiangardel on 17.05.16.
 */
public class CredentialDTO {
    private String username;
    private String password;
    private Boolean rememberMe;

    public UsernamePasswordToken daoToModel(String host)
    {
        UsernamePasswordToken tmp = new UsernamePasswordToken();
        tmp.setHost(host);
        if(rememberMe != null)
            tmp.setRememberMe(rememberMe);
        else
            tmp.setRememberMe(false);

        if(password != null)
            tmp.setPassword(password.toCharArray());
        else
            tmp.setPassword(null);

        tmp.setUsername(username);

        return tmp;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
