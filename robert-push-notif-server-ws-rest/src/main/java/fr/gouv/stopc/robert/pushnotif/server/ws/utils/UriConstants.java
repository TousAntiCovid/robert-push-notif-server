package fr.gouv.stopc.robert.pushnotif.server.ws.utils;

public final class UriConstants {

    private UriConstants() {

        throw new AssertionError();
    }

    public static final String PATH = "/push-token";

    public static final String API_V1 = "/v1";

    public static final String TOKEN_PATH_VARIABLE = "/{token}";

}
