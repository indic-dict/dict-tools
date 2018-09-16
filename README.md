

- [Dictionary and packaging software developer instructions](#dictionary-and-packaging-software-developer-instructions)
   - [Update /bin used by dictionary users](#update-/bin-used-by-dictionary-users)
   - [Links to general comments](#links-to-general-comments)

# dict-tools
Tools to process (mainly Indic) dictionaries.

# User instructions
## Command line interface: See src/main/scala/stardict_sanskrit/commandInterface to see what's available.

Example invocation:
```
java -jar bin/artifacts/dict-tools.jar install /home/vvasuki/sanskrit-coders/stardict-dicts-installed/ https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/dictionaryIndices.md
```

# Dictionary and packaging software developer instructions
## Update /bin used by dictionary users
To generate the bin/artifacts/dict-tools.jar artifact, which is used by dictionary developers during packaging:

- Use intellij?
- sbt assembly  

## Links to general comments
See [indic-transliteration/README](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md) for the following info:

  - [Setup](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#setup)
  - [Deployment](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#deployment)
    - [Regarding **maven targets** in intellij](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#regarding-**maven-targets**-in-intellij)
    - [Releasing to maven.](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#releasing-to-maven.)
    - [Building a jar.](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#building-a-jar.)
  - [Technical choices](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#technical-choices)
    - [Scala](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#scala)
