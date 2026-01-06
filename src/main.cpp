#include "Game.h"

#ifdef PLATFORM_ANDROID
#include <android_native_app_glue.h>

void android_main(struct android_app* state) {
    Game game;
    
    if (game.Initialize()) {
        game.Run();
    }
    
    game.Shutdown();
}

#elif defined(PLATFORM_IOS)
// iOS entry point would be in Objective-C/Swift wrapper

#else
// Desktop entry point for testing
int main(int argc, char* argv[]) {
    Game game;
    
    if (game.Initialize()) {
        game.Run();
    }
    
    game.Shutdown();
    return 0;
}
#endif
