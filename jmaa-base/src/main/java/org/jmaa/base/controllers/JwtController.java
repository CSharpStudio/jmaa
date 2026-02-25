package org.jmaa.base.controllers;

import org.jmaa.sdk.https.Controller;
import org.jmaa.sdk.https.RequestHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@org.springframework.web.bind.annotation.CrossOrigin
@org.springframework.stereotype.Controller
public class JwtController extends Controller {
    @RequestMapping(value = "/*/jwt", method = RequestMethod.POST)
    @RequestHandler(auth = RequestHandler.AuthType.NONE, type = RequestHandler.HandlerType.JSON)
    public Object getJwt(String login, String secret) {
        return getEnv().get("rbac.jwt").call("getJwt", login, secret);
    }
}
