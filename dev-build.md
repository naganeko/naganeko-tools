# how-to 

## WSL


### install WSL ( Linux on Windows )

- https://learn.microsoft.com/en-us/windows/wsl/install
- install Ubuntu 20.04 ( or just what you like )

### launch WSL console 

---
 The following description assumes that you enter it in the WSL console

## environment setup

### install leiningen
- https://leiningen.org/
```
sudo apt install leiningen
```
### install clojure
- https://clojure.org/
```
sudo apt install clojure
```

### install git 
```
sudo apt install git
```

### clone
```
# you can change the folder name as you wish.  
mkdir chibi-tools  
cd chibi-tools

git clone https://github.com/naganeko/naganeko-tools.git

cd naganeko-tools

```

## launch

- run figwheel

```
cd spine-gif-extract
lein figwheel dev
```

- open Chrome web browser on Windows ( not on WSL !!! )

  * access `http://localhost:3469/`
  * dev console ( ctrl-shift-i ) must be helpful

## build

```
# clean the compiled folder
rm -vrf resources/public/js/compiled/*

# cljsbuild
lein cljsbuild once min
```


