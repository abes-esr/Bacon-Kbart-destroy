# Bacon-Kbart-destroy

Guide court de lancement du programme.

## 1. Prerequis

- Java 8 ou plus
- Un fichier `config.properties` valide (base Oracle)

## 2. Compiler

### Windows (PowerShell)

```powershell
javac -cp ".;ojdbc8.jar;log4j-api-2.24.1.jar;log4j-core-2.24.1.jar;jar/org-apache-commons-lang.jar;jar/activation-1.1.jar" *.java
```

### Linux / Git Bash

```bash
javac -cp ".:ojdbc8.jar:log4j-api-2.24.1.jar:log4j-core-2.24.1.jar:jar/org-apache-commons-lang.jar:jar/activation-1.1.jar" *.java
```

## 3. Lancer

### Interface visuelle (recommande)

Windows:
```powershell
java -cp ".;ojdbc8.jar;log4j-api-2.24.1.jar;log4j-core-2.24.1.jar;jar/org-apache-commons-lang.jar;jar/activation-1.1.jar" EffacerPackageUI
```

Linux / Git Bash:
```bash
./EffacerPackageUI.sh
```

### Mode ligne de commande

Windows:
```powershell
java -cp ".;ojdbc8.jar;log4j-api-2.24.1.jar;log4j-core-2.24.1.jar;jar/org-apache-commons-lang.jar;jar/activation-1.1.jar" EffacerPackage
```

Linux / Git Bash:
```bash
./EffacerPackage.sh
```

## 4. Verification rapide

1. Lance l'interface.
2. Fais une recherche, par exemple `GALLICA_GLOBAL_ALL%`.
3. Verifie que des lignes apparaissent dans le tableau.

## 5. Package autonome (interface incluse)

Depuis PowerShell:

```powershell
./build-autonome.ps1
```

Le script genere un dossier `dist` contenant:
- `EffacerPackageUI-autonome.jar`
- `lib/` (dependances)
- `run-ui.bat` (Windows)
- `run-ui.sh` (Linux/Git Bash)

Pour lancer:

```powershell
cd dist
./run-ui.bat
```
