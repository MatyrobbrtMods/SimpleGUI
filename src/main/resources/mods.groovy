ModsDotGroovy.make {
    modLoader = 'javafml'
    loaderVersion = '[40,)'

    license = 'MIT'
    issueTrackerUrl = 'https://github.com/MatyrobbrtMods/SimpleGUI/issues'

    mod {
        modId = 'simplegui'
        displayName = 'SimpleGUI'
        updateJsonUrl = 'https://maven.moddinginquisition.org/releases/com/matyrobbrt/simplegui/simplegui/forge-promotions.json'

        version = this.version

        description = 'A library mod adding utilities for modern and simple GUIs.'
        authors = ['Matyrobbrt']

        // logoFile = 'simplegui.png'

        dependencies {
            forge = "[${this.forgeVersion},)"
            minecraft = this.minecraftVersionRange

            mod('jei') {
                mandatory = false
                versionRange = "[${this.buildProperties['jei_version']})"
            }
        }
    }
}