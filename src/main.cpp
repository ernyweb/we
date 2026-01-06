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
