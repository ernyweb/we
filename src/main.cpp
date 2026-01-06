#include "Game.h"
#include <memory>

#ifdef PLATFORM_ANDROID
#include <android_native_app_glue.h>
#include <android/log.h>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "Runner3D", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "Runner3D", __VA_ARGS__))

static void handle_cmd(struct android_app* app, int32_t cmd) {
    Game* game = (Game*)app->userData;
    
    switch (cmd) {
        case APP_CMD_INIT_WINDOW:
            LOGI("APP_CMD_INIT_WINDOW");
            if (app->window != nullptr && game != nullptr) {
                if (!game->Initialize()) {
                    LOGE("Game initialization failed!");
                }
            }
            break;
        case APP_CMD_TERM_WINDOW:
            LOGI("APP_CMD_TERM_WINDOW");
            if (game != nullptr) {
                game->Shutdown();
            }
            break;
        case APP_CMD_GAINED_FOCUS:
            LOGI("APP_CMD_GAINED_FOCUS");
            break;
        case APP_CMD_LOST_FOCUS:
            LOGI("APP_CMD_LOST_FOCUS");
            break;
    }
}

void android_main(struct android_app* state) {
    LOGI("android_main started");
    
    std::unique_ptr<Game> game = std::make_unique<Game>();
    state->userData = game.get();
    state->onAppCmd = handle_cmd;
    
    LOGI("Entering main loop");
    
    while (true) {
        int events;
        struct android_poll_source* source;
        
        while (ALooper_pollAll(0, nullptr, &events, (void**)&source) >= 0) {
            if (source != nullptr) {
                source->process(state, source);
            }
            
            if (state->destroyRequested != 0) {
                LOGI("Destroy requested");
                game->Shutdown();
                return;
            }
        }
        
        // Game loop - only run if initialized
        static bool initialized = false;
        if (state->window != nullptr && !initialized) {
            LOGI("Attempting to initialize game...");
            initialized = game->Initialize();
            if (!initialized) {
                LOGE("Failed to initialize game");
            }
        }
        
        if (initialized) {
            game->RunFrame();
        }
    }
}

#elif defined(PLATFORM_IOS)
// iOS entry point - minimal UIApplication setup
#include <UIKit/UIKit.h>

@interface AppDelegate : UIResponder <UIApplicationDelegate>
@property (nonatomic, strong) UIWindow *window;
@end

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    self.window = [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    self.window.backgroundColor = [UIColor blackColor];
    
    UIViewController *viewController = [[UIViewController alloc] init];
    self.window.rootViewController = viewController;
    [self.window makeKeyAndVisible];
    
    // Initialize and run game in a background thread to avoid blocking UI
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        static Game game;
        if (game.Initialize()) {
            game.Run();
        }
        game.Shutdown();
    });
    
    return YES;
}

@end

int main(int argc, char * argv[]) {
    @autoreleasepool {
        return UIApplicationMain(argc, argv, nil, NSStringFromClass([AppDelegate class]));
    }
}

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
