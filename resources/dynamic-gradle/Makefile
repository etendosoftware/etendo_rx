PHONY: build push

build:
	docker buildx build --platform=linux/amd64 -t etendo/dynamic-gradle:1.0.0 .

push:
	docker push etendo/dynamic-gradle:1.0.0
