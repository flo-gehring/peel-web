import jsPDF from 'jspdf'

type RenderDocumentPdfParams = {
  title: string
  body: string
}

export async function renderDocumentPdf({ title, body }: RenderDocumentPdfParams): Promise<string> {
  const doc = new jsPDF({
    orientation: 'portrait',
    unit: 'pt',
    format: 'a4',
  })

  const pageWidth = doc.internal.pageSize.getWidth()
  const pageHeight = doc.internal.pageSize.getHeight()
  const margin = 56
  const maxTextWidth = pageWidth - margin * 2
  const lineHeight = 16

  doc.setFont('helvetica', 'bold')
  doc.setFontSize(18)
  doc.text(title.trim().length > 0 ? title : 'Untitled document', margin, margin)

  doc.setFont('helvetica', 'normal')
  doc.setFontSize(12)

  const renderedBody = body.trim().length > 0 ? body : ' '
  const lines = doc.splitTextToSize(renderedBody, maxTextWidth) as string[]

  let cursorY = margin + 28
  for (const line of lines) {
    if (cursorY > pageHeight - margin) {
      doc.addPage()
      cursorY = margin
    }
    doc.text(line, margin, cursorY)
    cursorY += lineHeight
  }

  const blob = doc.output('blob')
  return URL.createObjectURL(blob)
}
