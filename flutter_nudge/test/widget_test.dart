import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_nudge/main.dart';

void main() {
  const channel = MethodChannel('com.focusguard.app/stats');

  void mockStats(Future<Object?> Function(MethodCall) handler) {
    final messenger = TestDefaultBinaryMessengerBinding
        .instance.defaultBinaryMessenger;
    messenger.setMockMethodCallHandler(channel, handler);
    addTearDown(() => messenger.setMockMethodCallHandler(channel, null));
  }

  testWidgets('shows recorded usage and score', (tester) async {
    mockStats((call) async {
      expect(call.method, 'getDailyStats');
      return {
        'totalScreenSeconds': 3600,
        'distractionSeconds': 900,
        'blocksTriggered': 2,
        'focusScore': 75,
      };
    });
    await tester.pumpWidget(const MyApp());
    await tester.pumpAndSettle();
    expect(find.text('75/100'), findsOneWidget);
    expect(find.text('1h 0m'), findsOneWidget);
    expect(find.text('0h 15m'), findsOneWidget);
    expect(find.text('2'), findsOneWidget);
  });

  testWidgets('shows clear empty state', (tester) async {
    mockStats((_) async => {'totalScreenSeconds': 0});
    await tester.pumpWidget(const MyApp());
    await tester.pumpAndSettle();
    expect(find.text('No screen time recorded today.'), findsOneWidget);
  });

  testWidgets('recovers after the native read fails', (tester) async {
    var attempts = 0;
    mockStats((_) async {
      if (++attempts == 1) throw PlatformException(code: 'STATS_UNAVAILABLE');
      return {'totalScreenSeconds': 0};
    });
    await tester.pumpWidget(const MyApp());
    await tester.pumpAndSettle();
    expect(find.text('Could not load usage statistics.'), findsOneWidget);
    await tester.tap(find.text('Retry'));
    await tester.pumpAndSettle();
    expect(find.text('No screen time recorded today.'), findsOneWidget);
  });
}
