PHONY: build push

VERSION:=1.0.0


build:
	echo "Build amd64"
	docker buildx build --platform=linux/amd64 -t etendo/dynamic-gradle:$(VERSION)-amd64 .
	docker push etendo/dynamic-gradle:$(VERSION)-amd64
	echo "Build arm64"
	docker buildx build --platform=linux/arm64 -t etendo/dynamic-gradle:$(VERSION)-arm64 .
	docker push etendo/dynamic-gradle:$(VERSION)-arm64
	
	echo "Create manifest"
	docker pull etendo/dynamic-gradle:$(VERSION)-amd64
	docker pull etendo/dynamic-gradle:$(VERSION)-arm64
	docker manifest create etendo/dynamic-gradle:$(VERSION) etendo/dynamic-gradle:$(VERSION)-amd64 etendo/dynamic-gradle:$(VERSION)-arm64
	docker manifest annotate etendo/dynamic-gradle:$(VERSION) etendo/dynamic-gradle:$(VERSION)-amd64 --os linux --arch amd64
	docker manifest annotate etendo/dynamic-gradle:$(VERSION) etendo/dynamic-gradle:$(VERSION)-arm64 --os linux --arch arm64
	docker manifest push etendo/dynamic-gradle:$(VERSION)

