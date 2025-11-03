package energy.lux.uplux;

import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;

import java.util.Date;
import java.util.UUID;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.OctetKeyPair;

@AllArgsConstructor
public class JWTDecoder {
    // found at https://keycloak.zenmo.com/realms/zenmo/protocol/openid-connect/certs
    protected static String defaultJwk = """
            {
                "kid": "hN_HaBnYl-n3wae9APMVa9RaR6BdN-3eHAHvVZT_3So",
                "kty": "OKP",
                "alg": "EdDSA",
                "use": "sig",
                "crv": "Ed25519",
                "x": "jmcE7tddPunTe6SbvuaNaeMQJk0bOcdYey2YeU_8lyM"
            }
            """;

    protected @NonNull JWSVerifier jwsVerifier;

    public JWTDecoder() {
        this(createDefaultVerifier());
    }

    public UUID jwtToUserId(@NonNull String jwt) {
        try {
            val signedJwt = SignedJWT.parse(jwt);

            if (signedJwt.getJWTClaimsSet().getExpirationTime().before(new Date())) {
                throw new UpluxException("ID Token has expired at " + signedJwt.getJWTClaimsSet().getExpirationTime());
            }

            val isValid = jwsVerifier.verify(signedJwt.getHeader(), signedJwt.getSigningInput(), signedJwt.getSignature());
            if (!isValid) {
                throw new UpluxException("ID Token does not comply with public key");
            }

            return UUID.fromString(signedJwt.getJWTClaimsSet().getSubject());
        } catch (Exception e) {
            throw UpluxException.create("Failed to parse ID Token", e);
        }
    }

    private static String resolveJwk() {
        val envJwk = System.getenv("UPLUX_JWK");
        if (envJwk != null) {
            return envJwk;
        }

        return defaultJwk;
    }

    private static JWSVerifier createDefaultVerifier() {
        try {
            return new Ed25519Verifier(OctetKeyPair.parse(resolveJwk()));
        } catch (Exception e) {
            throw UpluxException.create("Failed to initialize JWT verifier", e);
        }
    }
}
