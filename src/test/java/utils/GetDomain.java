package utils;

public class GetDomain {
        private final String idm;
        private final String tenant;

        public GetDomain(String idm, String tenant) {
            this.idm = idm;
            this.tenant = tenant;
        }

        public String getIDM() {
            return idm;
        }

        public String getTenant() {
            return tenant;
        }
}
