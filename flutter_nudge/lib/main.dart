import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(const MyApp());

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'FocusGuard',
      theme: ThemeData(colorSchemeSeed: const Color(0xFF6750A4)),
      home: const AiInsightsScreen(),
    );
  }
}

class AiInsightsScreen extends StatefulWidget {
  const AiInsightsScreen({super.key});

  @override
  State<AiInsightsScreen> createState() => _AiInsightsScreenState();
}

class _AiInsightsScreenState extends State<AiInsightsScreen> {
  static const _platform = MethodChannel('com.focusguard.app/stats');
  late Future<Map<String, Object?>> _stats = _readStats();

  Future<Map<String, Object?>> _readStats() async {
    final stats = await _platform.invokeMapMethod<String, Object?>(
      'getDailyStats',
    );
    if (stats == null) throw StateError('Empty response');
    return stats;
  }

  String _duration(int seconds) => '${seconds ~/ 3600}h ${seconds ~/ 60 % 60}m';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Focus Insights'),
        leading: IconButton(
          tooltip: 'Back',
          icon: const Icon(Icons.arrow_back),
          onPressed: () => SystemNavigator.pop(),
        ),
      ),
      body: SafeArea(
        child: FutureBuilder<Map<String, Object?>>(
          future: _stats,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) {
              return Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Text('Could not load usage statistics.'),
                    TextButton(
                      onPressed: () => setState(() => _stats = _readStats()),
                      child: const Text('Retry'),
                    ),
                  ],
                ),
              );
            }
            final stats = snapshot.requireData;
            final total = stats['totalScreenSeconds'] as int? ?? 0;
            if (total == 0) {
              return const Center(child: Text('No screen time recorded today.'));
            }
            final distractions = stats['distractionSeconds'] as int? ?? 0;
            final blocks = stats['blocksTriggered'] as int? ?? 0;
            final score = stats['focusScore'] as int?;
            return ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      children: [
                        const Text('Focus score'),
                        Text(
                          score == null ? '—' : '$score/100',
                          style: Theme.of(context).textTheme.displaySmall,
                        ),
                      ],
                    ),
                  ),
                ),
                ListTile(
                  leading: const Icon(Icons.timer_outlined),
                  title: const Text('Screen time'),
                  trailing: Text(_duration(total)),
                ),
                ListTile(
                  leading: const Icon(Icons.phone_android),
                  title: const Text('Time on Focus Breakers'),
                  trailing: Text(_duration(distractions)),
                ),
                ListTile(
                  leading: const Icon(Icons.shield_outlined),
                  title: const Text('Blocks shown'),
                  trailing: Text('$blocks'),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}
