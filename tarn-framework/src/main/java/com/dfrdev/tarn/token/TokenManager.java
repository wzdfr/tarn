package com.dfrdev.tarn.token;

import com.dfrdev.tarn.core.Controller;
import com.dfrdev.tarn.util.StringUtil;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * TokenManager.
 */
public class TokenManager {

    private static ITokenCache tokenCache;
    // 安全加固(Tarn 2.0+)：java.util.Random 可被预测，防伪造令牌改用 SecureRandom
    private static final SecureRandom random = new SecureRandom();
    public static String DEFAULT_TOKEN_NAME = "tarnframework_token";

    public static int DEFAULT_SECONDS_OF_TOKEN_TIME_OUT = 900;            // 900 seconds ---> 15 minutes

    public static int MIN_SECONDS_OF_TOKEN_TIME_OUT = 300;                // 300 seconds ---> 5 minutes

    private TokenManager() {

    }

    public static void init(ITokenCache tokenCache) {
        if (tokenCache == null)
            return;

        TokenManager.tokenCache = tokenCache;

        long halfTimeOut = MIN_SECONDS_OF_TOKEN_TIME_OUT * 1000 / 2;    // Token最小过期时间的一半时间作为任务运行的间隔时间
        new Timer().schedule(new TimerTask() {
                                 public void run() {
                                     removeTimeOutToken();
                                 }
                             },
                halfTimeOut,
                halfTimeOut);
    }

    /**
     * Create Token.
     *
     * @param Controller
     * @param tokenName        token name
     * @param secondsOfTimeOut seconds of time out, for ITokenCache only.
     */
    public static void createToken(Controller controller, String tokenName, int secondsOfTimeOut) {
        if (tokenCache == null) {
            String tokenId = String.valueOf(random.nextLong());
            controller.attr(tokenName, tokenId);
            controller.setSession(tokenName, tokenId);
            createTokenHiddenField(controller, tokenName, tokenId);
        } else {
            createTokenUseTokenIdGenerator(controller, tokenName, secondsOfTimeOut);
        }
    }

    /**
     * Use ${token!} in view for generate hidden input field.
     */
    private static void createTokenHiddenField(Controller controller, String tokenName, String tokenId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<input type='hidden' name='").append(tokenName).append("' value='" + tokenId).append("' />");
        controller.attr("token", sb.toString());
    }

    private static void createTokenUseTokenIdGenerator(Controller controller, String tokenName, int secondsOfTimeOut) {
        if (secondsOfTimeOut < MIN_SECONDS_OF_TOKEN_TIME_OUT)
            secondsOfTimeOut = MIN_SECONDS_OF_TOKEN_TIME_OUT;

        String tokenId = null;
        Token token = null;
        int safeCounter = 8;
        do {
            if (safeCounter-- == 0)
                throw new RuntimeException("Can not create tokenId.");
            tokenId = String.valueOf(random.nextLong());
            token = new Token(tokenId, System.currentTimeMillis() + (secondsOfTimeOut * 1000));
        } while (tokenId == null || tokenCache.contains(token));

        controller.attr(tokenName, tokenId);
        tokenCache.put(token);
        createTokenHiddenField(controller, tokenName, tokenId);
    }

    /**
     * Check token to prevent resubmit.
     *
     * @param tokenName the token name used in view's form
     * @return true if token is correct
     */
    public static synchronized boolean validateToken(Controller controller, String tokenName) {
        String clientTokenId = controller.get(tokenName);
        if (tokenCache == null) {
            String serverTokenId = controller.getSession(tokenName);
            controller.setSession(tokenName, null);        // important!
            return StringUtil.isNotEmpty(clientTokenId) && clientTokenId.equals(serverTokenId);
        } else {
            Token token = new Token(clientTokenId);
            boolean result = tokenCache.contains(token);
            tokenCache.remove(token);
            return result;
        }
    }

    private static void removeTimeOutToken() {
        List<Token> tokenInCache = tokenCache.getAll();
        if (tokenInCache == null)
            return;

        List<Token> timeOutTokens = new ArrayList<Token>();
        long currentTime = System.currentTimeMillis();
        // find and save all time out tokens
        for (Token token : tokenInCache)
            if (token.getExpirationTime() <= currentTime)
                timeOutTokens.add(token);

        // remove all time out tokens
        for (Token token : timeOutTokens)
            tokenCache.remove(token);
    }
}





