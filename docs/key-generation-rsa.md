
## Key Generation Guide

For production use, generate RSA key pairs using OpenSSL:

```bash
# Generate private key (2048-bit)
openssl genrsa -out jwt-private.pem 2048

# Extract public key
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem

# For higher security, use 4096-bit keys
openssl genrsa -out jwt-private.pem 4096
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem
```

Store these keys securely and reference them in your configuration.
