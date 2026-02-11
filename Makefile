
.PHONY: tag check-env

ENV := $(PWD)/.env
REPO := htts://repo.futit.cloud/maven-snapshots

include $(ENV)

define tag_rx
	git tag -a $(1) -m "Tagging version $(1)" && git push origin $(1) 
endef

define tag_mod
	cd modules/$(1) && git tag -a $(2) -m "Tagging version $(2)" && git push origin $(2) && cd ../.. && ./gradlew publishVersion -Ppkg=$(1) --info -Prepo=$(REPO)
endef

define del_tag
	cd modules/$(1) && git tag -d $(2) && git push origin :refs/tags/$(2)
endef

tag: 
	#$(call tag_mod,"com.etendorx.integration.obconnector","$(TAG)")
	#./gradlew :com.etendorx.integration.obconn.server:publish
	#./gradlew :com.etendorx.integration.obconn.worker:publish
	$(call tag_rx,"$(TAG)")

del-tag:
	$(call del_tag,"com.etendoerp.integration.to_openbravo","$(TAG)")

