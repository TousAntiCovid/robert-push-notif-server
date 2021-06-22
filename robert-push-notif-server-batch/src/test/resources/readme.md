.p8 file has been generated with : 

        openssl ecparam -name prime256v1 -genkey -noout | openssl pkcs8 -topk8 -nocrypt -out token-auth-private-key.p8