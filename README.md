# Battlecode 2020 Scaffold

This is the Battlecode 2020 scaffold, containing an `examplefuncsplayer`. Read https://2020.battlecode.org/getting-started!

### Project Structure

- `README.md`
    This file.
- `build.gradle`
    The Gradle build file used to build and run players.
- `src/`
    Player source code.
- `test/`
    Player test code.
- `client/`
    Contains the client.
- `build/`
    Contains compiled player code and other artifacts of the build process. Can be safely ignored.
- `matches/`
    The output folder for match files.
- `maps/`
    The default folder for custom maps.
- `gradlew`, `gradlew.bat`
    The Unix (OS X/Linux) and Windows versions, respectively, of the Gradle wrapper. These are nifty scripts that you can execute in a terminal to run the Gradle build tasks of this project. If you aren't planning to do command line development, these can be safely ignored.
- `gradle/`
    Contains files used by the Gradle wrapper scripts. Can be safely ignored.

### Resources

Here are some useful links for reference.

- Battlecode homepage:
https://2020.battlecode.org/home
- Battlecode repository:
https://github.com/skipiano/bc20
- Game specifications:
https://2020.battlecode.org/specs.html
- Java documentation:
https://2020.battlecode.org/javadoc/index.html
- Debugging:
[TODO: Not up yet, update when they fix the link]
- Game visualizer:
https://2020.battlecode.org/visualizer.html
- Bytecode optimization:
https://cory.li/bytecode-hacking/
- Git tutorial:
https://guides.github.com/introduction/git-handbook/

### Strategy

Our current strategy is to:
1. Produce miners
2. Find the nearest soup deposit and mine them
3. Somehow build a refinery? (Initially we have 200 soup and we used some of them on miners so we somoehow need to get up to 200 more soup until the miners can build them again) (so HQ is a refinery which I missed so we can just refine at HQ)
4. Let the miners keep mining and build landscaper factory (design schools) and drone factory (fullfillment centers)
5. Once with enough landscapers and drones, pick up necessary amounts of landscapers, drones, and one miner
6. Drop off the units once close to enemy HQ (We might need to build net guns in our base to counter this strategy too)
7. Build a net gun very close to the enemy HQ (3 blocks)
8. Surround enemy HQ with landscapers and bury to kill HQ
9. If there are units surrounding HQ so we can't bury HQ, build a river and flood HQ.

### Need to Fix

1. Improve scouting mechanism of searching for soup
2. How to check if the area is inacessible without landscapers?

3.create path objects each with an id(for future modification, need to be set unique in some way), so if nothing happend, just render the path.