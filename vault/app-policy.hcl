path "secret/*" {
	capabilities = ["read", "list"]
}

path "database/creds/readwrite" {
	capabilities = ["create", "read", "update", "delete", "list"]
}
path "database/creds/readwrite/*" {
        capabilities = ["create", "read", "update", "delete", "list"]
}
path "sys/leases/*" {
	capabilities = ["create", "read", "update", "delete", "list"]
}