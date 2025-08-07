# NVG

```bash
git log --oneline -n 10 --reflog
git checkout f48839f

git tag -a v1.5 -m "my version 1.5"
git push origin --tags
git tag
git show v1.5

git push --delete origin v1.5
```


```bash
$ git pull
erreur : impossible de tirer avec un rebasage : vous avez des modifications non indexées.
erreur : de plus, votre index contient des modifications non validées.
erreur : veuillez les valider ou les remiser.

# Correctif
$ git pull --rebase --autostash
```

