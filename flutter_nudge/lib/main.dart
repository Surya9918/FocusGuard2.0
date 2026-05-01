import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(const MyApp());

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'FocusGuard AI Insights',
      theme: ThemeData(
        useMaterial3: true,
        colorSchemeSeed: const Color(0xFF6750A4), // M3 Purple
        brightness: Brightness.light,
      ),
      home: const AiInsightsScreen(),
    );
  }
}

class AiInsightsScreen extends StatelessWidget {
  const AiInsightsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('AI Focus Insights'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            // This will close the Flutter Activity and return to the native side
            SystemNavigator.pop();
          },
        ),
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildHeaderCard(),
              const SizedBox(height: 24),
              const Text(
                'Deep Analysis',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 12),
              _buildInsightItem(
                icon: Icons.trending_up,
                color: Colors.green,
                title: 'Focus improved by 12%',
                description: 'You had fewer app-switches today compared to yesterday morning.',
              ),
              _buildInsightItem(
                icon: Icons.warning_amber_rounded,
                color: Colors.orange,
                title: 'Context Switching Risk',
                description: 'You tend to open "Instagram" right after "Slack". Try taking a 5-min break instead.',
              ),
              _buildInsightItem(
                icon: Icons.lightbulb_outline,
                color: Colors.blue,
                title: 'Peak Productivity',
                description: 'Your most focused hours are between 9 AM and 11 AM.',
              ),
              const SizedBox(height: 32),
              Center(
                child: OutlinedButton.icon(
                  onPressed: () => SystemNavigator.pop(),
                  icon: const Icon(Icons.exit_to_app),
                  label: const Text('Return to Native App'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeaderCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: const Color(0xFF6750A4).withOpacity(0.1),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFF6750A4).withOpacity(0.2)),
      ),
      child: Column(
        children: [
          const Icon(Icons.psychology, size: 64, color: Color(0xFF6750A4)),
          const SizedBox(height: 16),
          const Text(
            'AI Focus Score',
            style: TextStyle(fontSize: 18, color: Colors.black54),
          ),
          const Text(
            '88/100',
            style: TextStyle(fontSize: 48, fontWeight: FontWeight.bold, color: Color(0xFF6750A4)),
          ),
          const Text(
            'Excellent Resilience',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500, color: Colors.green),
          ),
        ],
      ),
    );
  }

  Widget _buildInsightItem({required IconData icon, required Color color, required String title, required String description}) {
    return Card(
      elevation: 0,
      color: Colors.grey[50],
      margin: const EdgeInsets.only(bottom: 12),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.grey[200]!),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            CircleAvatar(
              backgroundColor: color.withOpacity(0.1),
              child: Icon(icon, color: color),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 4),
                  Text(description, style: const TextStyle(color: Colors.black54)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
