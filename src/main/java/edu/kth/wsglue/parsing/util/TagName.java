package edu.kth.wsglue.parsing.util;

public class TagName {
        private String prefix;
        private String name;
        private String fullName;

        public TagName(String type) {
            String[] nameParts = type.split(":");
            if (nameParts.length == 2) {
                prefix = nameParts[0];
                name = nameParts[1];
                fullName = type;
            } else {
                prefix = null;
                fullName = name = type;
            }
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
    }