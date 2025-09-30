import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EmailTemplate, EmailTemplateService, TemplateStats } from '../../../services/email-template.service';
import { TemplateEditDialogComponent } from '../template-edit-dialog/template-edit-dialog.component';
import { TemplatePreviewDialogComponent } from '../template-preview-dialog/template-preview-dialog.component';


@Component({
  selector: 'app-email-templates',
  templateUrl: './email-templates.component.html',
  styleUrls: ['./email-templates.component.scss']
})
export class EmailTemplatesComponent implements OnInit {
  templates: EmailTemplate[] = [];
  filteredTemplates: EmailTemplate[] = [];
  templateStats: TemplateStats | null = null;
  
  loading = false;
  error = '';
  searchQuery = '';
  selectedCategory = '';
  selectedStatus = '';

  constructor(
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private templateService: EmailTemplateService
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
    this.loadStats();
  }

  loadTemplates(): void {
    this.loading = true;
    this.error = '';
    
    this.templateService.getAllTemplates().subscribe({
      next: (templates) => {
        this.templates = templates;
        this.applyFilter();
        this.loading = false;
      },
      error: (error) => {
        // Error loading templates
        this.error = 'Failed to load templates. Please try again.';
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    this.templateService.getTemplateStats().subscribe({
      next: (stats) => {
        this.templateStats = stats;
      },
      error: (error) => {
        // Error loading template stats
      }
    });
  }

  applyFilter(): void {
    this.filteredTemplates = this.templates.filter(template => {
      const matchesSearch = !this.searchQuery || 
        template.name.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        template.subject.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        template.category.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        (template.tags && template.tags.some(tag => tag.toLowerCase().includes(this.searchQuery.toLowerCase())));
      
      const matchesCategory = !this.selectedCategory || template.category === this.selectedCategory;
      const matchesStatus = !this.selectedStatus || template.status === this.selectedStatus;
      
      return matchesSearch && matchesCategory && matchesStatus;
    });
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.selectedCategory = '';
    this.selectedStatus = '';
    this.applyFilter();
  }

  createTemplate(): void {
    const dialogRef = this.dialog.open(TemplateEditDialogComponent, {
      width: '800px',
      disableClose: true,
      data: { mode: 'create' }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.snackBar.open('Template created successfully!', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.loadTemplates();
        this.loadStats();
      }
    });
  }

  editTemplate(template: EmailTemplate): void {
    const dialogRef = this.dialog.open(TemplateEditDialogComponent, {
      width: '800px',
      disableClose: true,
      data: { mode: 'edit', template: template }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.snackBar.open('Template updated successfully!', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.loadTemplates();
        this.loadStats();
      }
    });
  }

  duplicateTemplate(template: EmailTemplate): void {
    this.templateService.duplicateTemplate(template.id!).subscribe({
      next: (duplicated) => {
        this.snackBar.open(`Template duplicated: ${duplicated.name}`, 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.loadTemplates();
        this.loadStats();
      },
      error: (error) => {
        // Error duplicating template
        this.snackBar.open('Failed to duplicate template', 'Close', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      }
    });
  }

  previewTemplate(template: EmailTemplate): void {
    const dialogRef = this.dialog.open(TemplatePreviewDialogComponent, {
      width: '800px',
      data: { template: template }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result === 'use') {
        this.useTemplate(template);
      }
    });
  }

  useTemplate(template: EmailTemplate): void {
    this.templateService.useTemplate(template.id!).subscribe({
      next: () => {
        this.snackBar.open(`Template used: ${template.name}`, 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.loadTemplates();
        this.loadStats();
      },
      error: (error) => {
        // Error using template
        this.snackBar.open('Failed to record template usage', 'Close', {
          duration: 3000
        });
      }
    });
  }

  exportTemplate(template: EmailTemplate): void {
    const templateData = {
      name: template.name,
      subject: template.subject,
      body: template.body,
      category: template.category,
      tags: template.tags
    };
    
    const blob = new Blob([JSON.stringify(templateData, null, 2)], {
      type: 'application/json'
    });
    
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${template.name.replace(/[^a-z0-9]/gi, '_').toLowerCase()}.json`;
    link.click();
    window.URL.revokeObjectURL(url);
    
    this.snackBar.open(`Template exported: ${template.name}`, 'Close', {
      duration: 3000,
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }

  archiveTemplate(templateId: number | undefined): void {
    if (!templateId) return;
    const template = this.templates.find(t => t.id === templateId);
    if (template) {
      this.templateService.archiveTemplate(templateId).subscribe({
        next: () => {
          this.snackBar.open(`Archived template: ${template.name}`, 'Close', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
          this.loadTemplates();
          this.loadStats();
        },
        error: (error) => {
          // Error archiving template
          this.snackBar.open('Failed to archive template', 'Close', {
            duration: 5000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }
      });
    }
  }

  deleteTemplate(templateId: number | undefined): void {
    if (!templateId) return;
    const template = this.templates.find(t => t.id === templateId);
    if (template && confirm(`Are you sure you want to delete the template '${template.name}'? This action cannot be undone.`)) {
      this.templateService.deleteTemplate(templateId).subscribe({
        next: () => {
          this.snackBar.open(`Deleted template: ${template.name}`, 'Close', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
          this.loadTemplates();
          this.loadStats();
        },
        error: (error) => {
          // Error deleting template
          this.snackBar.open('Failed to delete template', 'Close', {
            duration: 5000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }
      });
    }
  }

  importTemplates(): void {
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = '.json';
    fileInput.style.display = 'none';
    
    fileInput.onchange = (event: any) => {
      const file = event.target.files[0];
      if (file) {
        const reader = new FileReader();
        reader.onload = (e) => {
          try {
            const templateData = JSON.parse(e.target?.result as string);
            const template: EmailTemplate = {
              name: templateData.name || 'Imported Template',
              subject: templateData.subject || '',
              body: templateData.body || '',
              category: templateData.category || 'OUTREACH',
              status: 'DRAFT',
              tags: templateData.tags || []
            };
            
            this.templateService.createTemplate(template).subscribe({
              next: (created) => {
                this.snackBar.open('Template imported successfully!', 'Close', {
                  duration: 3000,
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
                this.loadTemplates();
                this.loadStats();
              },
              error: (error) => {
                // Error importing template
                this.snackBar.open('Failed to import template', 'Close', {
                  duration: 5000,
                  horizontalPosition: 'right',
                  verticalPosition: 'top'
                });
              }
            });
          } catch (error) {
            this.snackBar.open('Invalid JSON file format', 'Close', {
              duration: 5000,
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
          }
        };
        reader.readAsText(file);
      }
    };
    
    document.body.appendChild(fileInput);
    fileInput.click();
    document.body.removeChild(fileInput);
  }

  sortTemplates(sortBy: 'name' | 'created' | 'used'): void {
    switch (sortBy) {
      case 'name':
        this.filteredTemplates.sort((a, b) => a.name.localeCompare(b.name));
        break;
      case 'created':
        this.filteredTemplates.sort((a, b) => {
          const aTime = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const bTime = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          return bTime - aTime;
        });
        break;
      case 'used':
        this.filteredTemplates.sort((a, b) => (b.usageCount || 0) - (a.usageCount || 0));
        break;
    }
  }

  exportAll(): void {
    const allTemplatesData = this.templates.map(template => ({
      name: template.name,
      subject: template.subject,
      body: template.body,
      category: template.category,
      tags: template.tags
    }));

    const blob = new Blob([JSON.stringify(allTemplatesData, null, 2)], {
      type: 'application/json'
    });

    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `all_templates_${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    window.URL.revokeObjectURL(url);

    this.snackBar.open('All templates exported successfully', 'Close', {
      duration: 3000,
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }

  getCategoryClass(category: string): string {
    switch (category) {
      case 'OUTREACH': return 'category-outreach';
      case 'FOLLOW_UP': return 'category-follow-up';
      case 'REFERRAL': return 'category-referral';
      case 'INTERVIEW': return 'category-interview';
      case 'THANK_YOU': return 'category-thank-you';
      default: return 'category-default';
    }
  }

  getCategoryIcon(category: string): string {
    switch (category) {
      case 'OUTREACH': return 'mail_outline';
      case 'FOLLOW_UP': return 'reply';
      case 'REFERRAL': return 'people_outline';
      case 'INTERVIEW': return 'event';
      case 'THANK_YOU': return 'thumb_up';
      default: return 'email';
    }
  }

  getTemplateStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'DRAFT': return 'status-draft';
      case 'ARCHIVED': return 'status-archived';
      default: return '';
    }
  }

  getPreviewText(body: string): string {
    return body.length > 150 ? body.substring(0, 150) + '...' : body;
  }
}