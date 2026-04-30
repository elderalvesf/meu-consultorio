import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import '../../data/models/prontuario_entry.dart';
import '../../providers/prontuario_provider.dart';
import '../../widgets/common_widgets.dart';

class ProntuarioFormScreen extends ConsumerStatefulWidget {
  final int patientId;
  final int? appointmentId;
  final int? entryId;
  const ProntuarioFormScreen({super.key, required this.patientId, this.appointmentId, this.entryId});

  @override
  ConsumerState<ProntuarioFormScreen> createState() => _ProntuarioFormScreenState();
}

class _ProntuarioFormScreenState extends ConsumerState<ProntuarioFormScreen> {
  final _textCtrl = TextEditingController();
  String? _localImagePath;
  String? _existingImageUrl;
  bool _isSaving = false;
  ProntuarioEntry? _existing;

  @override
  void initState() {
    super.initState();
    if (widget.entryId != null) _load();
  }

  void _load() {
    final entries = ref.read(prontuarioProvider).patientEntries;
    _existing = entries.where((e) => e.id == widget.entryId).firstOrNull;
    if (_existing != null) {
      _textCtrl.text = _existing!.text;
      _existingImageUrl = _existing!.imageUrl;
      _localImagePath = _existing!.imagePath;
      setState(() {});
    }
  }

  @override
  void dispose() {
    _textCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickImage(ImageSource source) async {
    final picker = ImagePicker();
    final picked = await picker.pickImage(source: source, imageQuality: 80);
    if (picked != null) setState(() => _localImagePath = picked.path);
  }

  Future<void> _save() async {
    if (_textCtrl.text.trim().isEmpty && _localImagePath == null && _existingImageUrl == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Adicione texto ou uma imagem')));
      return;
    }
    setState(() => _isSaving = true);
    final entry = ProntuarioEntry(
      id: _existing?.id ?? 0,
      patientId: widget.patientId,
      appointmentId: widget.appointmentId ?? _existing?.appointmentId,
      text: _textCtrl.text.trim(),
      imagePath: _localImagePath,
      imageUrl: _existingImageUrl,
      createdAt: _existing?.createdAt ?? DateTime.now().millisecondsSinceEpoch,
    );
    if (_existing == null) {
      await ref.read(prontuarioProvider.notifier).insert(entry);
    } else {
      await ref.read(prontuarioProvider.notifier).update(entry);
    }
    // Upload da imagem se houver nova imagem local
    if (_localImagePath != null && _localImagePath != _existing?.imagePath) {
      final savedEntry = ref.read(prontuarioProvider).patientEntries
          .where((e) => e.patientId == widget.patientId)
          .lastOrNull;
      if (savedEntry != null) {
        await ref.read(prontuarioProvider.notifier).uploadImage(savedEntry.id, _localImagePath!);
      }
    }
    if (mounted) { setState(() => _isSaving = false); Navigator.pop(context); }
  }

  @override
  Widget build(BuildContext context) {
    final isUploading = ref.watch(prontuarioProvider).isUploading;
    final cs = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.entryId == null ? 'Novo Registro' : 'Editar Registro'),
        backgroundColor: cs.primary,
        foregroundColor: cs.onPrimary,
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: (_isSaving || isUploading) ? null : _save,
        icon: (_isSaving || isUploading)
            ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
            : const Icon(Icons.save),
        label: Text(isUploading ? 'Enviando imagem...' : _isSaving ? 'Salvando...' : 'Salvar'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              controller: _textCtrl,
              decoration: const InputDecoration(
                labelText: 'Anotação / Evolução',
                prefixIcon: Icon(Icons.notes_outlined),
                alignLabelWithHint: true,
              ),
              maxLines: 6,
            ),
            const SizedBox(height: 16),
            Text('Imagem (opcional)', style: Theme.of(context).textTheme.titleSmall),
            const SizedBox(height: 8),
            // Imagem existente
            if (_existingImageUrl != null && _localImagePath == null)
              ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: Image.network(_existingImageUrl!, height: 200, width: double.infinity, fit: BoxFit.cover),
              ),
            // Imagem local nova
            if (_localImagePath != null)
              ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: Image.file(File(_localImagePath!), height: 200, width: double.infinity, fit: BoxFit.cover),
              ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _pickImage(ImageSource.camera),
                    icon: const Icon(Icons.camera_alt_outlined),
                    label: const Text('Câmera'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _pickImage(ImageSource.gallery),
                    icon: const Icon(Icons.photo_library_outlined),
                    label: const Text('Galeria'),
                  ),
                ),
              ],
            ),
            if (_localImagePath != null || _existingImageUrl != null)
              TextButton.icon(
                onPressed: () => setState(() { _localImagePath = null; _existingImageUrl = null; }),
                icon: Icon(Icons.delete_outline, color: cs.error),
                label: Text('Remover imagem', style: TextStyle(color: cs.error)),
              ),
            const SizedBox(height: 80),
          ],
        ),
      ),
    );
  }
}
