# naganeko-tools


## why?

recently I have no time to maintain them.


## lein 
install lein https://leiningen.org/ 


## how to launch ( dev-mode )
```shell 

cd spine-gif-extract 
lein figwheel dev

```

## build 

```shell 

cd spine-gif-extract 

# clear the compiled folder
rm -vrf resources/public/js/compiled/*

# cljsbuild
lein cljsbuild once min

```

## license

GPL V3 

