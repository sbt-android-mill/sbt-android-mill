## project name

please note that project name in .project must be the same as build.sbt ```name```

## dependency management

after change dependencies do ```sbt deliver-local```

install IvyDE, add ivy-0.1.xml to Eclipse project Properties -> Java Build Path -> Libraries -> Add library -> IvyDE Managed Dependencies

to add sources append ```source->source``` to dependency

<dependency org="NNNNN" name="NNNNNNN" rev="N.N.N" conf="compile->default(compile);source->source"/>

or use sbt-source-align
